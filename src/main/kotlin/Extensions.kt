import data.LessonInfo
import data.UserSchedule
import java.time.DayOfWeek


/**
 * this is used for week days translation
 */
val weekDaysMapName: Map<DayOfWeek, String> = mapOf(
    DayOfWeek.MONDAY to "Понедельник",

    DayOfWeek.TUESDAY to "Вторник",

    DayOfWeek.WEDNESDAY to "Среда",

    DayOfWeek.THURSDAY to "Четверг",

    DayOfWeek.FRIDAY to "Пятница",

    DayOfWeek.SATURDAY to "Суббота",

    DayOfWeek.SUNDAY to "Воскресенье"
)

/**
 * this is used to get week day name, which program can use
 */
val stringToWeekDaysMap: Map<String, DayOfWeek> = mapOf(
    "поне" to DayOfWeek.MONDAY,

    "вт" to DayOfWeek.TUESDAY,

    "ср" to DayOfWeek.WEDNESDAY,

    "чт" to DayOfWeek.THURSDAY,

    "пт" to DayOfWeek.FRIDAY,

    "сб" to DayOfWeek.SATURDAY,

    "вс" to DayOfWeek.SUNDAY,
)

/**
 * Adds Russian translation to week days
 */
fun DayOfWeek?.russianName(): String {
    return weekDaysMapName[this] ?: ""
}

/**
 * it is used to interpreter day of week's names and store them in a better format
 * (used for pinning messages (@see processPin))
 * @param text representation of weekday in Russian
 */
fun getDay(text: String): DayOfWeek? {
    text.lowercase().let {
        stringToWeekDaysMap.onEach { (t, u) ->
            if (it.contains(t))
                return u
        }
    }
    return null
}

/**
 * it is used to understand if we need to edit a message
 * (if our new text matches the previous one, it throws Telegram API error)
 * @param compare - new schedule we want to compare
 * (we compare everything except for messageInfo)
 */
fun UserSchedule?.matchesWith(compare: UserSchedule): Boolean {
    if (this == null || this.messages.isEmpty()) return false
    this.messages.forEachIndexed { index, message ->
        val compareMessage = compare.messages[index]
        if (message.dayOfWeek != compareMessage.dayOfWeek) {
            return false
        }
        message.lessonInfo.forEachIndexed { index1, lessonInfo ->
            return lessonInfo.matchesWith(compareMessage.lessonInfo[index1])
        }
    }
    return true
}

/**
 * this is used to understand is LessonInfo matches a given one
 * @param compare this one we are comparing to
 */
fun LessonInfo.matchesWith(compare: LessonInfo): Boolean {
    return !(this.lesson != compare.lesson || this.classroom != compare.classroom || this.teacher != compare.teacher)
}
