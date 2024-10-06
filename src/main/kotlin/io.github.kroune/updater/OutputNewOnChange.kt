package io.github.kroune.updater

import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.ParseMode
import eu.vendeli.tgbot.types.chat.Chat
import eu.vendeli.tgbot.types.internal.getOrNull
import io.github.kroune.Notifier.transformToMessage
import io.github.kroune.ScheduleUpdater
import io.github.kroune.bot
import io.github.kroune.configurationRepository
import kotlinx.serialization.Serializable

/**
 * outputs schedule, when it changes, ignoring previous schedule
 */
@Serializable
class OutputNewOnChange : UpdateI {
    override val modeName: String = "OutputNewOnChange"

    override suspend fun notifyUserAboutChanges(
        chat: Chat,
    ) {
        val watchedClass = configurationRepository.getWatchedClassForChat(chat)!!
        val schedule = ScheduleUpdater.schedule[watchedClass]!!
        val newMessages = transformToMessage(schedule)
        val messagesIds = newMessages.map {
            message(it).options {
                this.parseMode = ParseMode.HTML
            }.sendReturning(chat, bot).await().getOrNull()!!.messageId
        }
        configurationRepository.setOldMessageIds(chat, messagesIds)
    }

    override suspend fun reOutput(chat: Chat) {
        val watchedClass = configurationRepository.getWatchedClassForChat(chat)!!
        val schedule = ScheduleUpdater.schedule[watchedClass]!!
        val newMessages = transformToMessage(schedule)
        val messagesIds = newMessages.map {
            message(it).options {
                this.parseMode = ParseMode.HTML
            }.sendReturning(chat, bot).await().getOrNull()!!.messageId
        }
        configurationRepository.setOldMessageIds(chat, messagesIds)
    }

    override fun onChangedModeFrom(chat: Chat) {}
}
