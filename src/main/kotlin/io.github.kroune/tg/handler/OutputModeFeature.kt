@file:Suppress("MatchingDeclarationName")

package io.github.kroune.tg.handler

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.annotations.CommandHandler
import eu.vendeli.tgbot.annotations.InputHandler
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.interfaces.helper.Guard
import eu.vendeli.tgbot.types.User
import eu.vendeli.tgbot.types.chat.Chat
import eu.vendeli.tgbot.types.internal.CallbackQueryUpdate
import eu.vendeli.tgbot.types.internal.MessageUpdate
import eu.vendeli.tgbot.types.internal.ProcessedUpdate
import io.github.kroune.configurationRepository
import io.github.kroune.translationRepository
import io.github.kroune.updater.UpdateI
import kotlin.reflect.full.createInstance


/**
 * outputs general info about available output modes and output list of all available ones them
 */
@CommandHandler(["/output_mode"])
suspend fun changeOutputMode(update: MessageUpdate, user: User, bot: TelegramBot) {
    val chat = update.message.chat
    message { translationRepository.outputModeChangeResponse }.send(chat, bot)
    val possibleModes = UpdateI::class.sealedSubclasses.map {
        it.createInstance().modeName
    }
    message {
        "${translationRepository.outputModeList} \n${possibleModes.joinToString(separator = "\n")}"
    }.inlineKeyboardMarkup {
        possibleModes.forEach {
            it callback it
            newLine()
        }
    }.send(chat, bot)
    bot.inputListener.set(user) {
        "outputModeSelection"
    }
}

/**
 * @return false if user message != one of output modes
 */
class OutputModeSelectionGuard : Guard {
    override suspend fun condition(
        user: User?,
        update: ProcessedUpdate,
        bot: TelegramBot
    ): Boolean {
        val chat: Chat?
        val text: String?
        when (update) {
            is MessageUpdate -> {
                chat = update.origin.message?.chat
                text = update.text
            }

            is CallbackQueryUpdate -> {
                chat = update.callbackQuery.message?.chat
                text = update.callbackQuery.data
            }

            else -> {
                return false
            }
        }
        if (chat == null || text == null) {
            return false
        }
        val isFound = UpdateI::class.sealedSubclasses.any {
            it.createInstance().modeName.lowercase() == text.lowercase()
        }
        if (!isFound) {
            message { translationRepository.outputModeNotFound }.send(chat, bot)
        }
        return isFound
    }
}

/**
 * changes output mode for the user
 */
@InputHandler(["outputModeSelection"], guard = OutputModeSelectionGuard::class)
suspend fun selectOutputMode(update: ProcessedUpdate, bot: TelegramBot) {
    val chat: Chat
    val text: String
    when (update) {
        is MessageUpdate -> {
            chat = update.message.chat
            text = update.text
        }

        is CallbackQueryUpdate -> {
            chat = update.callbackQuery.message?.chat ?: return
            text = update.text
        }

        else -> {
            return
        }
    }
    val outputMode = UpdateI::class.sealedSubclasses.first() {
        it.createInstance().modeName.lowercase() == text.lowercase()
    }.createInstance()
    message { translationRepository.outputModeChangeSuccess }.send(chat, bot)
    configurationRepository.getOutputMode(chat).onChangedModeFrom(chat)
    configurationRepository.setOutputMode(chat, outputMode)
    configurationRepository.getWatchedClassForChat(chat)?.let {
        outputMode.reOutput(
            chat
        )
    }
}
