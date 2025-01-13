package io.github.kroune.tg.handler

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.annotations.UnprocessedHandler
import eu.vendeli.tgbot.api.message.forwardMessage
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.User
import eu.vendeli.tgbot.types.internal.MessageUpdate
import io.github.kroune.ADMIN_TG_ALERT_CHAT_ID

@UnprocessedHandler
suspend fun unprocessedHandler(update: MessageUpdate, user: User, bot: TelegramBot) {
    message {
        buildString {
            appendLine("user - $user")
            appendLine("chat - ${update.message.chat}")
        }
    }.send(ADMIN_TG_ALERT_CHAT_ID, bot)
    // debug logging
    forwardMessage(update.message.chat, update.message.messageId).send(ADMIN_TG_ALERT_CHAT_ID, bot)
}
