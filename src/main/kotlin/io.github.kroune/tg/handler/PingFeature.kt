package tg.handler

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.annotations.CommandHandler
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.internal.MessageUpdate

/**
 * Simple pin - pong command to check if bot is running
 */
@CommandHandler(["/ping"])
suspend fun ping(update: MessageUpdate, bot: TelegramBot) {
    message { "pong" }.send(update.message.chat, bot)
}
