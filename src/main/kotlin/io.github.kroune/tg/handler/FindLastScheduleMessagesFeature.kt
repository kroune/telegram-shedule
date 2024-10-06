package io.github.kroune.tg.handler

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.annotations.CommandHandler
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.internal.MessageUpdate
import eu.vendeli.tgbot.types.internal.onFailure
import io.github.kroune.alert
import io.github.kroune.configurationRepository
import io.github.kroune.translationRepository

/**
 * Re outputs schedule, forgetting previous messages
 */
@CommandHandler(["/find_schedule"])
suspend fun findLastScheduleMessages(update: MessageUpdate, bot: TelegramBot) {
    val messageToReply = configurationRepository.getOldMessageIds(update.message.chat).firstOrNull()
    if (messageToReply == null) {
        message { translationRepository.oldMessagesWereNotFound }.send(update.message.chat, bot)
        return
    }
    val messageSendingResult = message { translationRepository.done }
        .options {
            replyToMessageId = messageToReply
        }
        .sendReturning(update.message.chat, bot)
    messageSendingResult.await().onFailure {
        if (it.errorCode == 400 && it.description == "Bad Request: message to be replied not found") {
            message { translationRepository.oldMessagesWereNotFound }.send(update.message.chat, bot)
            return
        }
        alert("finding old message failed. code - ${it.errorCode}, description - ${it.description}")
    }
}
