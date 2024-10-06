package io.github.kroune.updater

import eu.vendeli.tgbot.types.chat.Chat
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
     */
    suspend fun notifyUserAboutChanges(
        chat: Chat
    )

    /**
     * should only be called if the user has chosen class to watch
     * forces re output of the schedule
     * @param chat chat
     */
    suspend fun reOutput(
        chat: Chat
    )

    /**
     * @param chat chat
     * what to do if user has switched from this mode
     */
    fun onChangedModeFrom(chat: Chat)
}
