package io.github.kroune.updater

import eu.vendeli.tgbot.api.message.editMessageText
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.ParseMode
import eu.vendeli.tgbot.types.chat.Chat
import eu.vendeli.tgbot.types.internal.getOrNull
import eu.vendeli.tgbot.types.internal.onFailure
import io.github.kroune.EDITED_MESSAGE_IS_THE_SAME_TG_ERROR
import io.github.kroune.Notifier.transformToMessage
import io.github.kroune.ScheduleUpdater
import io.github.kroune.bot
import io.github.kroune.configurationRepository
import io.github.kroune.translationRepository
import io.github.kroune.unparsedScheduleParser.Lessons
import kotlinx.datetime.DayOfWeek
import kotlinx.serialization.Serializable

/**
 * If old messages with schedule exist, then edit them, otherwise send new ones.
 */
@Serializable
class EditPreviousOnChange : UpdateI {
    override val modeName: String = "EditPreviousOnChange"

    override suspend fun notifyUserAboutChanges(
        chat: Chat
    ) {
        val watchedClass = configurationRepository.getWatchedClassForChat(chat)!!
        val schedule = ScheduleUpdater.schedule[watchedClass]!!
        val oldMessages = configurationRepository.getOldMessageIds(chat)
        val newMessages = transformToMessage(schedule)
        if (newMessages.size == oldMessages.size) {
            var isSuccess = true
            run {
                oldMessages.forEachIndexed { index, messageId ->
                    editMessageText(messageId) {
                        newMessages[index]
                    }.options {
                        this.parseMode = ParseMode.HTML
                    }.sendReturning(chat, bot).await().onFailure {
                        // we will get this if schedules matches for a curtain day, that's ok
                        if (it.errorCode == 400 && it.description == EDITED_MESSAGE_IS_THE_SAME_TG_ERROR) {
                            return@onFailure
                        }
                        isSuccess = false
                        return@run
                    }
                }
                message { translationRepository.scheduleHasChanged }.options {
                    replyToMessageId = oldMessages.first()
                }.send(chat, bot)
            }
            if (!isSuccess) {
                message(translationRepository.editingOldMessagesFailedCommaResending).send(chat, bot)
                sendNewMessages(chat, schedule)
            }
        } else {
            sendNewMessages(chat, schedule)
        }
    }

    override suspend fun reOutput(chat: Chat) {
        val watchedClass = configurationRepository.getWatchedClassForChat(chat)!!
        val schedule = ScheduleUpdater.schedule[watchedClass]!!
        sendNewMessages(chat, schedule)
    }

    override fun onChangedModeFrom(chat: Chat) {}

    /**
     * sends new messages, will happen when old are inaccessible or when mode is re chosen
     */
    suspend fun sendNewMessages(chat: Chat, newSchedule: Map<DayOfWeek, Lessons>) {
        val newMessages = transformToMessage(newSchedule)
        val messagesIds = newMessages.map {
            message(it).options {
                this.parseMode = ParseMode.HTML
            }.sendReturning(chat, bot).await().getOrNull()!!.messageId
        }
        configurationRepository.setOldMessageIds(chat, messagesIds)
    }
}
