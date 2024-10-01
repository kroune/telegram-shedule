package tg.handler

import data.translationRepository
import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.annotations.CommandHandler
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.internal.MessageUpdate

/**
 * displays general information about bot
 */
@CommandHandler(["/info"])
suspend fun displayHelpInfo(update: MessageUpdate, bot: TelegramBot) {
    message { translationRepository.botInfo }.send(update.message.chat.id, bot)
}
