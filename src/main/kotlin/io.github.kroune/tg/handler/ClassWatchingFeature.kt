@file:Suppress("MatchingDeclarationName")

package tg.handler

import Schedule.availableClasses
import Schedule.currentUpdateJob
import data.configurationRepository
import data.translationRepository
import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.annotations.CommandHandler
import eu.vendeli.tgbot.annotations.InputHandler
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.interfaces.helper.Guard
import eu.vendeli.tgbot.types.User
import eu.vendeli.tgbot.types.internal.MessageUpdate
import eu.vendeli.tgbot.types.internal.ProcessedUpdate

/**
 * @return false if there is no such class
 */
class ClassSelectionGuard : Guard {
    override suspend fun condition(
        user: User?,
        update: ProcessedUpdate,
        bot: TelegramBot
    ): Boolean {
        val chatState = update.origin.message
        if (user == null || chatState == null)
            return false
        val chat = chatState.chat
        currentUpdateJob?.join()
        if (availableClasses?.contains(update.text.uppercase()) != true) {
            message { translationRepository.classNotFoundResponse }.send(chat, bot)
            message(translationRepository.availableClassesList).send(chat, bot)
            message { availableClasses?.joinToString("\n") ?: "" }.send(chat, bot)
            return false
        }
        return true
    }
}

/**
 * outputs general info about choosing class to watch
 */
@InputHandler(["classSelection"], guard = ClassSelectionGuard::class)
suspend fun selectClass(update: MessageUpdate, user: User, bot: TelegramBot) {
    message { translationRepository.classSelectedResponse }.send(update.message.chat, bot)
    // wait if we are currently processing an update
    configurationRepository.setUserWatchedClass(update.message.chat, update.text.uppercase())
    bot.inputListener.del(user.id)
}

/**
 * updates class you are watching
 */
@CommandHandler(["/class"])
suspend fun changeClass(update: MessageUpdate, user: User, bot: TelegramBot) {
    message { translationRepository.classResponse }.send(update.message.chat, bot)
    bot.inputListener.set(user) { "classSelection" }
}
