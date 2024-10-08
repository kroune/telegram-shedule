package io.github.kroune.tg.handler

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.annotations.CommandHandler
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.internal.MessageUpdate
import io.github.kroune.translationRepository

/**
 * Provides general info about bot and what you should do
 */
@CommandHandler(["/start"])
suspend fun start(update: MessageUpdate, bot: TelegramBot) {
    message { translationRepository.startResponse }.send(update.message.chat, bot)
}
