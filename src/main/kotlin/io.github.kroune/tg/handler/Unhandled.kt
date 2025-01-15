package io.github.kroune.tg.handler

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.annotations.UnprocessedHandler
import eu.vendeli.tgbot.api.message.forwardMessage
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.ParseMode
import eu.vendeli.tgbot.types.User
import eu.vendeli.tgbot.types.internal.MessageUpdate
import eu.vendeli.tgbot.types.internal.getOrNull
import io.github.kroune.ADMIN_TG_ALERT_CHAT_ID

/**
 * used for debug purpose
 */
@UnprocessedHandler
suspend fun unprocessedHandler(update: MessageUpdate, user: User, bot: TelegramBot) {
    // debug logging
    val messageResult =
        forwardMessage(update.message.chat, update.message.messageId).sendReturning(ADMIN_TG_ALERT_CHAT_ID, bot).await()
            .getOrNull()
    if (messageResult == null)
        return
    message {
        buildString {
            appendLine("user - $user")
            appendLine("chat - ${update.message.chat}")
        }
    }.options {
        this.replyToMessageId = messageResult.messageId
        this.parseMode = ParseMode.HTML
    }.send(ADMIN_TG_ALERT_CHAT_ID, bot)
}
