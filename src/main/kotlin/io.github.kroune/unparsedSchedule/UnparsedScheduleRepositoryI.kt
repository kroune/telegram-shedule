package io.github.kroune.unparsedSchedule

/**
 * Gets unparsed schedule data
 */
sealed interface UnparsedScheduleRepositoryI {
    /**
     * @return schedule as a 2d matrix of strings
     */
    suspend fun getSchedule(): List<List<String>>
}
