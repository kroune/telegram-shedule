package io.github.kroune.configuration

import eu.vendeli.tgbot.types.chat.Chat
import io.github.kroune.unparsedScheduleParser.ClassName
import io.github.kroune.updater.UpdateI

/**
 * Interface for bot configuration
 */
sealed interface ConfigurationRepositoryI {
    /**
     * @param chat chat
     * @param newClassName name of the class (11Д, 10Мб 5А). All chars should be uppercase
     * updates the class user is watching and notifies user about new schedule
     */
    fun setUserWatchedClass(chat: Chat, newClassName: String)

    /**
     * @param className name of the class (11Д, 10Мб 5А). All chars should be uppercase
     * @returns list of users watching this class
     */
    fun getClassWatchers(className: ClassName): List<Chat>

    /**
     * @param chat chat
     * @return class user is watching or null if user is not watching any class
     */
    fun getWatchedClassForChat(chat: Chat): ClassName?

    /**
     * @param chat chat
     * @param outputMode output mode, defines how we should notify user about schedule changes
     */
    fun setOutputMode(chat: Chat, outputMode: UpdateI)

    /**
     * @param chat chat
     * @return output mode of the user or [io.github.kroune.defaultOutputMode] if it wasn't overridden yet
     */
    fun getOutputMode(chat: Chat): UpdateI

    fun getChats(): List<Chat>

    /**
     * deletes user from the configuration
     */
    fun deleteChat(chat: Chat)

    /**
     * changes old messages ids to new ones
     */
    fun setOldMessageIds(chat: Chat, oldMessages: List<Long>)

    /**
     * gets id of old messages with schedule we send
     */
    fun getOldMessageIds(chat: Chat): List<Long>
}
