package io.github.kroune.updater

import eu.vendeli.tgbot.types.chat.Chat
import io.github.kroune.unparsedScheduleParser.Lessons
import kotlinx.datetime.DayOfWeek
import kotlinx.serialization.Serializable

/**
 * Interface for different types of notifying about schedule changes
 */
@Serializable
sealed interface UpdateI {
    /**
     * pretty name of the mode
     * it will be sent in the list of available modes
     * @see [io.github.kroune.tg.handler.changeOutputMode]
     */
    val modeName: String

    /**
     * @param chat chat
     * @param oldSchedule old schedule of this user or empty map if user hasn't received schedule yet
     * @param newSchedule new schedule of this user
     */
    fun notifyUserAboutChanges(
        chat: Chat,
        oldSchedule: Map<DayOfWeek, Lessons>,
        newSchedule: Map<DayOfWeek, Lessons>
    )
}