import data.parserRepository
import data.unparsedScheduleParser.ClassName
import data.unparsedScheduleParser.Lessons
import data.unparsedScheduleRepository
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
object Schedule {
    /**
     * object for safer use of current schedule
     */
    object CurrentSchedule {
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
    var currentSchedule: Map<ClassName, Map<DayOfWeek, Lessons>>
        get() {
            return CurrentSchedule.getSchedule()
        }
        set(value) {
            CurrentSchedule.setSchedule(value)
        }

    private fun save() {
        val configDir = File(CONFIGURATION_DIRECTORY)
        configDir.mkdirs()
        val configFile = File(CONFIGURATION_DIRECTORY, "schedule.json")
        if (!configFile.exists()) {
            configFile.createNewFile()
        }
        val text = jsonClient.encodeToString<Map<ClassName, Map<DayOfWeek, Lessons>>>(currentSchedule)
        configFile.writeText(text)
    }

    init {
        val configDir = File(CONFIGURATION_DIRECTORY)
        configDir.mkdirs()
        val configFile = File(CONFIGURATION_DIRECTORY, "schedule.json")
        if (configFile.exists()) {
            jsonClient.decodeFromString<Map<ClassName, Map<DayOfWeek, Lessons>>>(configFile.readText()).let {
                CurrentSchedule.setScheduleWithoutNotifications(it)
            }
        }
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

    private suspend fun CoroutineScope.syncChanges() {
        currentUpdateJob = launch {
            val unparsedSchedule = unparsedScheduleRepository.getSchedule()
            availableClasses = parserRepository.getClassesNames(unparsedSchedule).map { it.second }
            currentSchedule = parserRepository.parse(unparsedSchedule)
        }
        currentUpdateJob?.join()
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                syncChanges()
                delay(5.minutes)
            }
        }
    }
}
