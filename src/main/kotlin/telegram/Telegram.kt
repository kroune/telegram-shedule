package telegram

import IS_TEST
import com.elbekd.bot.Bot
import com.elbekd.bot.model.TelegramApiError
import com.elbekd.bot.model.toChatId
import data.*
import data.Config.configs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * telegram bot, initialized on start
 */
val bot: Bot = Bot.createPolling(if (IS_TEST) TEST_TOKEN else PRODUCTION_TOKEN)

/**
 * sends async message in telegram chat
 * @param chatId ID of telegram chat
 * @param text is a string we want to output
 */
fun sendAsyncMessage(chatId: Long, text: String): Long {
    var id = -1L
    CoroutineScope(Dispatchers.Default).launch {
        try {
            id = bot.sendMessage(chatId.toChatId(), text).messageId
        } catch (e: Exception) {
            println("An exception has occurred while sending message to chat $chatId")
            println(e.stackTraceToString())
            println("text is \n$text")
        }
    }
    return id
}

/**
 * sends message in telegram chat
 * @param chatId ID of telegram chat
 * @param text is a string we want to output
 */
suspend fun sendMessage(chatId: Long, text: String): Long {
    return try {
        bot.sendMessage(chatId.toChatId(), text).messageId
    } catch (e: Exception) {
        println("An exception has occurred while sending message to chat $chatId")
        println(e.stackTraceToString())
        println("text is \n$text")
        -1L
    }
}

/**
 * this is used to check if a message exists
 * @param chatId ID of telegram chat
 */
suspend fun Long.exists(chatId: Long): Boolean {
    return try {
        bot.copyMessage((-1L).toChatId(), chatId.toChatId(), this)
        true
    } catch (e: TelegramApiError) {
        if (e.description.contains("chat not found")) true
        else if (e.description.contains("message to copy not found")) {
            false
        } else {
            error(chatId, "unexpected error checking if message exists \n ${e.stackTraceToString()}")
            false
        }
    }
}

/**
 * This is used to understand what stage program is
 */
enum class Result {
    /**
     * this is used when we can't manage pinned messages
     */
    NotEnoughRight,

    /**
     * this means chat was deleted or chat was corrupted
     */
    ChatNotFound,

    /**
     * this means a message was deleted
     */
    MessageNotFound,

    /**
     * this shouldn't happen normally
     */
    Error,

    /**
     * if no error was thrown
     */
    Success
}

/**
 * it is used to pin only schedule for the current day
 * @param chatId ID of telegram chat
 */
suspend fun pinRequiredMessage(chatId: Long): Pair<Result, Boolean> {
    var attemptedToChangePinState = false
    debug(chatId, "updating pinned message")
    try {
        val day = LocalDate.now().dayOfWeek
        configs[chatId]!!.schedule.messages.forEach { message ->
            if (message.messageInfo.messageId == -1L) {
                return Pair(Result.MessageNotFound, false)
            }
            // if we need to change pin state
            if (day == message.dayOfWeek && !message.messageInfo.pinState) {
                attemptedToChangePinState = true
                bot.pinChatMessage(chatId.toChatId(), message.messageInfo.messageId, true)
                message.messageInfo.pinState = true
            }

            if (day != message.dayOfWeek && message.messageInfo.pinState) {
                attemptedToChangePinState = true
                bot.unpinChatMessage(chatId.toChatId(), message.messageInfo.messageId)
                message.messageInfo.pinState = false
            }
        }
    } catch (e: TelegramApiError) {
        debug(chatId, "exception caught \n ${e.stackTraceToString()}")
        telegramApiErrorMap.onEach {
            if (e.description.contains(it.key)) return Pair(it.value, false)
        }
        error(chatId, "unexpected telegram api error was thrown \n ${e.stackTraceToString()}")
        return Pair(Result.Error, false)
    }
    return Pair(Result.Success, attemptedToChangePinState)
}

/**
 * this is used for processing pin/unpin function
 */
val telegramApiErrorMap: Map<String, Result> = mapOf(
    "not enough rights" to Result.NotEnoughRight,
    "chat not found" to Result.ChatNotFound,
    "message to pin not found" to Result.MessageNotFound
)
