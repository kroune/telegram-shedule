package io.github.kroune

import io.github.kroune.unparsedScheduleParser.Lessons
import kotlinx.datetime.DayOfWeek

/**
 * defines how we should notify user
 */
object Notifier {
    /**
     * transforms schedule to messages
     * @return list of messages to be sent to user using MarkDown2
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
                    "${lesson.first} - <i>${lesson.second}</i> ${
                        if (lesson.third != null)
                            " - ${lesson.third}"
                        else
                            ""
                    }"
                )
            }
            val weekdayName = with(translationRepository) {
                day.nameInLocalLang()
            }
            messages.add("<b>$weekdayName</b>:\n${lessonInfo.joinToString(separator = "\n")}")
        }
        return messages
    }
}
