import java.time.DayOfWeek


fun DayOfWeek.getRussianName(): String {
    when (this) {
        DayOfWeek.MONDAY -> {
            return "Понедельник"
        }

        DayOfWeek.TUESDAY -> {
            return "Вторник"
        }

        DayOfWeek.WEDNESDAY -> {
            return "Среда"
        }

        DayOfWeek.THURSDAY -> {
            return "Четверг"
        }

        DayOfWeek.FRIDAY -> {
            return "Пятница"
        }

        DayOfWeek.SATURDAY -> {
            return "Суббота"
        }

        DayOfWeek.SUNDAY -> {
            return "Воскресенье"
        }
    }
}

fun getDate(idk: String): DayOfWeek? {
    idk.lowercase().let {
        if (it.contains("поне")) {
            return DayOfWeek.MONDAY
        }
        if (it.contains("вт")) {
            return DayOfWeek.TUESDAY
        }
        if (it.contains("ср")) {
            return DayOfWeek.WEDNESDAY
        }
        if (it.contains("чет")) {
            return DayOfWeek.THURSDAY
        }
        if (it.contains("пят")) {
            return DayOfWeek.FRIDAY
        }
        if (it.contains("суб")) {
            return DayOfWeek.SATURDAY
        }
        if (it.contains("вос")) {
            return DayOfWeek.SUNDAY
        }
    }
    return null
}

fun MutableList<Triple<DayOfWeek?, MutableList<Triple<String, String, String>>, Pair<Long, Boolean>>>.matchesWith(
    compare: MutableList<Triple<DayOfWeek?, MutableList<Triple<String, String, String>>, Pair<Long, Boolean>>>?
): Boolean {
    if (compare == null) return false
    this.forEachIndexed { index, (first, second, _) ->
        if (first != compare[index].first || second != compare[index].second) return false
    }
    return true
}
