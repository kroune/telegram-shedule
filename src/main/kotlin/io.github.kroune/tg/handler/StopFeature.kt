package tg.handler

import data.configurationRepository
import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.annotations.CommandHandler
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.internal.MessageUpdate


/**
 * Simple pin - pong command to check if bot is running
 */
@CommandHandler(["/stop"])
suspend fun stop(update: MessageUpdate, bot: TelegramBot) {
    configurationRepository.deleteChat(update.message.chat)
    message { "Bot stopped" }.send(update.message.chat, bot)
}
