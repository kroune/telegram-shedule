package io.github.kroune.updater

import eu.vendeli.tgbot.api.message.editMessageText
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.chat.Chat
import eu.vendeli.tgbot.types.internal.getOrNull
import eu.vendeli.tgbot.types.internal.onFailure
import io.github.kroune.EDITED_MESSAGE_IS_THE_SAME_TG_ERROR
import io.github.kroune.Notifier.transformToMessage
import io.github.kroune.bot
import io.github.kroune.configurationRepository
import io.github.kroune.translationRepository
import io.github.kroune.unparsedScheduleParser.Lessons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.serialization.Serializable

/**
 * If old messages with schedule exist, then edit them, otherwise send new ones.
 */
@Serializable
class EditPreviousOnChange : UpdateI {
    override val modeName: String = "EditPreviousOnChange"

    override fun notifyUserAboutChanges(
        chat: Chat,
        oldSchedule: Map<DayOfWeek, Lessons>,
        newSchedule: Map<DayOfWeek, Lessons>
    ) {
        val oldMessages = configurationRepository.getOldMessageIds(chat)
        val newMessages = transformToMessage(newSchedule)
        if (newMessages.size == oldMessages.size) {
            var isSuccess = true
            CoroutineScope(Dispatchers.IO).launch {
                oldMessages.forEachIndexed { index, messageId ->
                    editMessageText(messageId) {
                        newMessages[index]
                    }.sendReturning(chat, bot).await().onFailure {
                        // we will get this if schedules matches for a curtain day, that's ok
                        if (it.errorCode == 400 && it.description == EDITED_MESSAGE_IS_THE_SAME_TG_ERROR) {
                            return@onFailure
                        }
                        isSuccess = false
                        return@launch
                    }
                }
            }
            if (!isSuccess) {
                CoroutineScope(Dispatchers.IO).launch {
                    message(translationRepository.editingOldMessagesFailedCommaResending).send(chat, bot)
                    sendNewMessages(chat, newSchedule)
                }
            }
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                sendNewMessages(chat, newSchedule)
            }
        }
    }

    /**
     * sends new messages, will happen when old are inaccessible or when mode is re chosen
     */
    suspend fun sendNewMessages(chat: Chat, newSchedule: Map<DayOfWeek, Lessons>) {
        val newMessages = transformToMessage(newSchedule)
        val messagesIds = newMessages.map {
            message(it).sendReturning(chat, bot).await().getOrNull()!!.messageId
        }
        configurationRepository.setOldMessageIds(chat, messagesIds)
    }
}
