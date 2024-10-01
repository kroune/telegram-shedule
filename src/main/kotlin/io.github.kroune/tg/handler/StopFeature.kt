package io.github.kroune.tg.handler

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.annotations.CommandHandler
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.internal.MessageUpdate
import io.github.kroune.configurationRepository


/**
 * Simple pin - pong command to check if bot is running
 */
@CommandHandler(["/stop"])
suspend fun stop(update: MessageUpdate, bot: TelegramBot) {
    configurationRepository.deleteChat(update.message.chat)
    message { "Bot stopped" }.send(update.message.chat, bot)
}
