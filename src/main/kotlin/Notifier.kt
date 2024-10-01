import data.configurationRepository
import data.translationRepository
import data.unparsedScheduleParser.ClassName
import data.unparsedScheduleParser.Lessons
import kotlinx.datetime.DayOfWeek

/**
 * defines how we should notify user
 */
object Notifier {
    /**
     * transforms schedule to messages
     */
    fun transformToMessage(schedule: Map<DayOfWeek, Lessons>): MutableList<String> {
        if (schedule.isEmpty()) {
            return mutableListOf()
        }
        val messages: MutableList<String> = mutableListOf()
        DayOfWeek.entries.forEach { day ->
            val lessonInfo = mutableListOf<String>()
            schedule[day]?.forEach { lesson ->
                if (lesson == null) {
                    lessonInfo.add("")
                    return@forEach
                }
                lessonInfo.add(
                    "${lesson.first} - ${lesson.second} ${if (lesson.third != null) " - ${lesson.third}" else ""}"
                )
            }
            val weekdayName = with(translationRepository) {
                day.nameInLocalLang()
            }
            messages.add("$weekdayName:\n${lessonInfo.joinToString(separator = "\n")}")
        }
        return messages
    }

    /**
     * handles schedule updates for entire class
     */
    fun processScheduleChange(
        className: ClassName,
        oldSchedule: Map<DayOfWeek, Lessons>,
        newSchedule: Map<DayOfWeek, Lessons>
    ) {
        configurationRepository.getClassWatchers(className).forEach { chat ->
            // notify according to selected mode
            configurationRepository.getOutputMode(chat).notifyUserAboutChanges(chat, oldSchedule, newSchedule)
        }
    }
}
