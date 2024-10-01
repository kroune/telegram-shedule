package io.github.kroune.unparsedScheduleParser

import io.github.kroune.dropWhileInclusive
import io.github.kroune.mapWithIndexes
import io.github.kroune.repeatedAdd
import io.github.kroune.unsafeFilterTo
import kotlinx.datetime.DayOfWeek
import kotlin.collections.getOrNull
import kotlin.text.isNullOrEmpty
import kotlin.text.toInt
import kotlin.text.toIntOrNull

/**
 * Parses raw schedule data from google sheets api
 */
class ParserRepositoryImpl : ParserRepositoryI {
    override fun getClassesNames(data: List<List<String>>): List<Pair<Int, ClassName>> {
        val rowWithIndexes = data.first {
            it.any { it.lowercase().contains("время") }
        }.mapWithIndexes()
        val classStringsWithIndexes = rowWithIndexes.dropWhileInclusive { (_, element) ->
            !element.lowercase().contains("время")
        }.filter { (_, element) ->
            element.isNotEmpty()
        }
        return classStringsWithIndexes.map { (index, element) ->
            Pair(index, element.uppercase())
        }
    }

    private fun getWeekDay(str: String?): DayOfWeek? {
        if (str == null || str.length < 3) return null
        return when (str.lowercase().subSequence(0..2)) {
            "пон" -> DayOfWeek.MONDAY
            "вто" -> DayOfWeek.TUESDAY
            "сре" -> DayOfWeek.WEDNESDAY
            "чет" -> DayOfWeek.THURSDAY
            "пят" -> DayOfWeek.FRIDAY
            "суб" -> DayOfWeek.SATURDAY
            "вос" -> DayOfWeek.SUNDAY
            else -> null
        }
    }

    /**
     * @param data raw schedule
     * @return column, where lesson index (1, 2, 3...) is located
     */
    fun getLessonsColumnIndexes(data: List<List<String>>): Int {
        return (0..<data.size).indexOfFirst { offset ->
            data.any {
                it.getOrNull(offset) == "№"
            }
        }
    }

    /**
     * @param data raw schedule
     * @return list of pairs (range of indexes that belong from weekday to the next one, weekday)
     */
    fun getWeekDaysIndexesRanges(data: List<List<String>>): List<Pair<IntRange, DayOfWeek>> {
        val maxIndex = data.maxOf { it.size }
        var weekDaysColumnIndex: Int? = null
        for (columnIndex in 0..<maxIndex) {
            for (rowIndex in 0 until data.size) {
                if (getWeekDay(data[rowIndex].getOrNull(columnIndex)) != null) {
                    weekDaysColumnIndex = columnIndex
                    break
                }
            }
        }
        require(weekDaysColumnIndex != null)
        val weekDaysWithIndexes = data.map { it.getOrNull(weekDaysColumnIndex) }.mapWithIndexes {
            getWeekDay(it)
        }.unsafeFilterTo(mutableListOf<Pair<Int, DayOfWeek>>()) { (_, element) ->
            element != null
        }
        return weekDaysWithIndexes.mapIndexed { dayIndex, (index, weekDay) ->
            val endIndex = (weekDaysWithIndexes.getOrNull(dayIndex + 1)?.first ?: (data.size - 1))
            Pair(index..<endIndex, weekDay)
        }
    }

    private fun getLessonsInfo(rowIndex: Int, columnIndex: Int, data: List<List<String>>): Pair<Int, Lesson>? {
        val lessonsColumnIndex = getLessonsColumnIndexes(data)
        val lessonNumber = data[rowIndex].getOrNull(lessonsColumnIndex)
        val subject = data[rowIndex].getOrNull(columnIndex)
        val teacher = data[rowIndex].getOrNull(columnIndex + 1)
        val classroom = data[rowIndex + 1].getOrNull(columnIndex + 1)
        if (!subject.isNullOrEmpty() && teacher != null && lessonNumber?.toIntOrNull() != null) {
            return Pair(lessonNumber.toInt(), Triple(subject, teacher, classroom))
        }
        return null
    }

    override fun parse(
        data: List<List<String>>
    ): Result<MutableMap<ClassName, MutableMap<DayOfWeek, Lessons>>> {
        return runCatching {
            val weekDaysIndexesRanges = getWeekDaysIndexesRanges(data)
            val schedule: MutableMap<ClassName, MutableMap<DayOfWeek, Lessons>> = mutableMapOf()

            getClassesNames(data).forEach { (columnIndex, className) ->
                weekDaysIndexesRanges.forEach { (range, weekDay) ->
                    // map of class name to schedule for this day
                    val weekDaySchedule: Lessons = mutableListOf()

                    range.forEach { rowIndex ->
                        val lessonInfo = getLessonsInfo(rowIndex, columnIndex, data) ?: return@forEach
                        weekDaySchedule.repeatedAdd(null, lessonInfo.first - (weekDaySchedule.size + 1))
                        weekDaySchedule.add(lessonInfo.second)
                    }
                    schedule.getOrPut(className) {
                        mutableMapOf<DayOfWeek, Lessons>()
                    }[weekDay] = weekDaySchedule
                }
            }
            schedule
        }
    }
}
