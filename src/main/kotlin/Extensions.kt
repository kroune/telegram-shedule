import data.LogLevel
import data.UserSchedule
import data.log
import java.time.DayOfWeek


/**
 * Adds Russian translation to week days
 */
fun DayOfWeek?.russianName(): String {
    when (this) {
        DayOfWeek.MONDAY -> return "Понедельник"

        DayOfWeek.TUESDAY -> return "Вторник"

        DayOfWeek.WEDNESDAY -> return "Среда"

        DayOfWeek.THURSDAY -> return "Четверг"

        DayOfWeek.FRIDAY -> return "Пятница"

        DayOfWeek.SATURDAY -> return "Суббота"

        DayOfWeek.SUNDAY -> return "Воскресенье"

        null -> return ""
    }
}

/**
 * it is used to interpreter day of week's names and store them in a better format
 * (used for pinning messages (@see processPin))
 * @param text - representation of weekday in Russian
 */
@Suppress("SpellCheckingInspection", "RedundantSuppression")
fun getDay(text: String, chatId: Long): DayOfWeek? {
    text.lowercase().let {
        if (it.contains("поне")) return DayOfWeek.MONDAY

        if (it.contains("вт")) return DayOfWeek.TUESDAY

        if (it.contains("ср")) return DayOfWeek.WEDNESDAY

        if (it.contains("чет")) return DayOfWeek.THURSDAY

        if (it.contains("пят")) return DayOfWeek.FRIDAY

        if (it.contains("суб")) return DayOfWeek.SATURDAY

        if (it.contains("вос")) return DayOfWeek.SUNDAY
    }
    log(chatId, "Error recognizing week day name - $text", LogLevel.Error)
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
            if (lessonInfo.lesson != compareMessage.lessonInfo[index1].lesson ||
                lessonInfo.classroom != compareMessage.lessonInfo[index1].classroom ||
                lessonInfo.teacher != compareMessage.lessonInfo[index1].teacher
            ) {
                return false
            }
        }
    }
    return true
}