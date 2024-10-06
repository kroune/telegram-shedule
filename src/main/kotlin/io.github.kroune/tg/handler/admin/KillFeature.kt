package io.github.kroune.tg.handler.admin

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.annotations.CommandHandler
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.internal.MessageUpdate
import io.github.kroune.ADMIN_TG_CHAT_ID
import kotlin.system.exitProcess


/**
 * kill switch, only I can execute this command
 */
@CommandHandler(["/kill"])
suspend fun kill(update: MessageUpdate, bot: TelegramBot) {
    if (update.message.chat.id != ADMIN_TG_CHAT_ID) {
        return
    }
    message { "Bot stopped" }.send(update.message.chat, bot)
    exitProcess(0)
}
