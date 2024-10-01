package io.github.kroune.tg.handler

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.annotations.CommandHandler
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.internal.MessageUpdate
import io.github.kroune.VERSION

/**
 * Outputs information about bot version
 */
@CommandHandler(["/version"])
suspend fun version(update: MessageUpdate, bot: TelegramBot) {
    message { VERSION.toString() }.send(update.message.chat, bot)
}
