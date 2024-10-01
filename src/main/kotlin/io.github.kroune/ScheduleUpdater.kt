package io.github.kroune

import io.github.kroune.unparsedScheduleParser.ClassName
import io.github.kroune.unparsedScheduleParser.Lessons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.serialization.encodeToString
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.time.Duration.Companion.minutes

/**
 * Currently stores schedule info and updates it every 5 minutes
 */
object ScheduleUpdater {
    /**
     * object for safer use of current schedule
     */
    object Schedule {
        private var privateCurrentSchedule: Map<ClassName, Map<DayOfWeek, Lessons>> = mapOf()

        /**
         * updates schedule without notifications, used when initializing
         */
        fun setScheduleWithoutNotifications(schedule: Map<ClassName, Map<DayOfWeek, Lessons>>) {
            privateCurrentSchedule = schedule
        }

        /**
         * updates schedule and notifies about changes
         */
        fun setSchedule(schedule: Map<ClassName, Map<DayOfWeek, Lessons>>) {
            schedule.forEach { (className, schedule) ->
                val anyChanges = schedule != (privateCurrentSchedule[className] ?: mapOf<DayOfWeek, Lessons>())
                if (anyChanges) {
                    Notifier.processScheduleChange(className, privateCurrentSchedule[className] ?: mapOf(), schedule)
                }
            }
            privateCurrentSchedule = schedule
            save()
        }

        /**
         * just returns current schedule
         */
        fun getSchedule(): Map<ClassName, Map<DayOfWeek, Lessons>> {
            return privateCurrentSchedule
        }
    }

    /**
     * current schedule
     */
    var schedule
        get() = Schedule.getSchedule()
        set(value) = Schedule.setSchedule(value)

    private fun save() {
        {
            val configDir = File(CONFIGURATION_DIRECTORY)
            configDir.mkdirs()
            val configFile = File(CONFIGURATION_DIRECTORY, "schedule.json")
            if (!configFile.exists()) {
                configFile.createNewFile()
            }
            val text = jsonClient.encodeToString<Map<ClassName, Map<DayOfWeek, Lessons>>>(schedule)
            configFile.writeText(text)
        }.retryableExitedOnFatal(20)
    }


    /**
     * list of available classes schedule (all chars are uppercase)
     */
    var availableClasses: List<ClassName>? = null

    /**
     * currently working coroutine for schedule update
     * it is used wait until we finish fetching schedule, when [availableClasses] is null
     */
    var currentUpdateJob: Job? = null

    init {
        {
            val configDir = File(CONFIGURATION_DIRECTORY)
            configDir.mkdirs()
            val configFile = File(CONFIGURATION_DIRECTORY, "schedule.json")
            if (configFile.exists()) {
                jsonClient.decodeFromString<Map<ClassName, Map<DayOfWeek, Lessons>>>(configFile.readText()).let {
                    Schedule.setScheduleWithoutNotifications(it)
                }
            }
        }.retryableExitedOnFatal(20)
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                suspend {
                    currentUpdateJob = launch {
                        val unparsedSchedule = unparsedScheduleRepository.getSchedule()
                        availableClasses = parserRepository.getClassesNames(unparsedSchedule).map { it.second }
                        schedule = parserRepository.parse(unparsedSchedule).getOrThrow()
                    }
                    currentUpdateJob?.join()
                    delay(5.minutes)
                }.retryableExitedOnFatal(retries = 20, delay = 1.minutes)
            }
        }
    }
}
