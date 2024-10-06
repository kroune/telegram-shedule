package io.github.kroune.tg.handler

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.annotations.CommandHandler
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.internal.MessageUpdate
import io.github.kroune.configurationRepository
import io.github.kroune.translationRepository

/**
 * Re outputs schedule, forgetting previous messages
 */
@CommandHandler(["/output_schedule"])
suspend fun outputSchedule(update: MessageUpdate, bot: TelegramBot) {
    if (configurationRepository.getWatchedClassForChat(update.message.chat) == null) {
        message { translationRepository.youHaveNotChosenClassToWatchYet }.send(update.message.chat, bot)
        return
    }
    configurationRepository.getOutputMode(update.message.chat).reOutput(update.message.chat)
}
