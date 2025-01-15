package io.github.kroune.tg.handler.admin

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.annotations.CommandHandler
import eu.vendeli.tgbot.annotations.InputHandler
import eu.vendeli.tgbot.api.message.forwardMessage
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.internal.MessageUpdate
import io.github.kroune.ADMIN_TG_CHAT_ID
import io.github.kroune.configurationRepository

/**
 * used to send important notifications to all users
 */
@CommandHandler(["/notify_everybody"])
suspend fun notifyEverybody(update: MessageUpdate, bot: TelegramBot) {
    if (update.message.chat.id != ADMIN_TG_CHAT_ID) {
        return
    }
    message { "send your message" }.send(update.message.chat, bot)
    bot.inputListener.set(ADMIN_TG_CHAT_ID, "notify_everybody")
}

/**
 * second part of sending notification to all users
 */
@InputHandler(["notify_everybody"])
suspend fun sendMessage(update: MessageUpdate, bot: TelegramBot) {
    configurationRepository.getChats().forEach {
        forwardMessage(update.message.chat.id, update.message.messageId).send(it, bot)
    }
    message { "notification was sent" }.send(update.message.chat, bot)
}
