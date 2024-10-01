package data.unparsedScheduleParser

import kotlinx.datetime.DayOfWeek

typealias Subject = String
typealias Teacher = String
typealias Classroom = String?
typealias Lesson = Triple<Subject, Teacher, Classroom>
typealias Lessons = MutableList<Lesson?>
typealias ClassName = String

/**
 * Parses schedule info from data
 */
sealed interface ParserRepositoryI {
    /**
     * @param data raw schedule
     * @return list of pairs (index, class name) from data
     */
    fun getClassesNames(data: List<List<String>>): List<Pair<Int, ClassName>>

    /**
     * @param data raw schedule
     * @return map of class name to (map of weekday to lessons)
     */
    fun parse(data: List<List<String>>): MutableMap<ClassName, MutableMap<DayOfWeek, Lessons>>
}
