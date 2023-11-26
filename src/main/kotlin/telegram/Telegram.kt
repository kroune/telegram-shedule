package telegram

import com.elbekd.bot.Bot
import com.elbekd.bot.model.TelegramApiError
import com.elbekd.bot.model.toChatId
import data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import matchesWith
import russianName
import java.time.LocalDate

/**
 * it creates a telegram bot, using provided token
 */
val bot: Bot = Bot.createPolling(TOKEN)

/**
 * it displays all data in the chat like this:
 *
 *   Monday
 *  PE {в 13} (Ivan Ivanov)
 *  Math {в 1} (Ivan Nikolay)
 *  etc...
 * @param chatId ID of telegram chat
 * @param shouldResendMessage we use it if a user does /output
 */
suspend fun UserSchedule.displayInChat(chatId: Long, shouldResendMessage: Boolean) {
    log(chatId, "outputting schedule data", LogLevel.Info)

    // we do this backwards, so we don't output non-existing lessons, while keeping info about first ones
    this@displayInChat.messages.forEachIndexed { index, message ->
        var werePrevious = false
        var messageText = ""

        message.lessonInfo.reversed().forEach { info ->
            when (info.lesson) {
                "" -> if (werePrevious) messageText = "\n $messageText"

                else -> {
                    messageText = "${info.lesson} {в ${info.classroom}} (${info.teacher}) \n $messageText"
                    werePrevious = true
                }
            }
        }

        messageText = " ${message.dayOfWeek.russianName()} \n $messageText"

        if (!shouldResendMessage && storedSchedule[chatId] != null && (storedSchedule[chatId] ?: return).messages.isNotEmpty()
            && (storedSchedule[chatId] ?: return).messages.all {
                it.messageInfo.messageId != -1L
            }
        ) {
            if (!(storedSchedule[chatId] ?: return).matchesWith(this)) {
                try {
                    val id = bot.editMessageText(
                        chatId.toChatId(),
                        (storedSchedule[chatId] ?: return).messages[index].messageInfo.messageId,
                        text = messageText
                    )
                    message.messageInfo = MessageInfo(id.messageId, false)

                } catch (e: Exception) {
                    log(chatId, "error ${(storedSchedule[chatId] ?: return).messages} $e", LogLevel.Error)
                }
            }
        } else {
            val id = sendMessage(chatId, messageText)
            message.messageInfo = MessageInfo(id, false)
        }
    }
    storedSchedule[chatId] = this
    storeConfigs(chatId)
}

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
 */
suspend fun Long.exists(chatId: Long): Boolean {
    return try {
        bot.copyMessage("-1".toLong().toChatId(), chatId.toChatId(), this)
        true
    } catch (e: TelegramApiError) {
        if (e.description.contains("chat not found")) true
        else if (e.description.contains("message to copy not found")) {
            false
        } else {
            log(chatId, "unexpected error while checking is message exists \n $e", LogLevel.Error)
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
 * this function processes schedule pinging and handle errors
 * @param chatId ID of telegram chat
 */
suspend fun processSchedulePinning(chatId: Long) {
    when (pinRequiredMessage(chatId)) {
        Result.NotEnoughRight -> {
            log(chatId, "not enough rights", LogLevel.Info)
            if (!(pinErrorShown[chatId] ?: return)) {
                log(chatId, "outputting rights warning", LogLevel.Debug)
                pinErrorShown[chatId] = true
                sendMessage(
                    chatId, "не достаточно прав для закрепления сообщения"
                )
                storeConfigs(chatId)
            }
        }

        Result.ChatNotFound -> {
            log(chatId, "chat with id $chatId was deleted", LogLevel.Info)
            deleteData(chatId)
        }

        Result.Error -> {
            log(chatId, "an error has occurred", LogLevel.Error)
            //TODO: add error counter and retry
        }

        Result.MessageNotFound -> {
        }

        Result.Success -> {
            log(chatId, "successfully updated pinned messages", LogLevel.Info)
            if ((pinErrorShown[chatId] ?: return)) {
                pinErrorShown[chatId] = false
            }
            storeConfigs(chatId)
        }
    }
}

/**
 * it is used to pin only schedule for the current day
 */
suspend fun pinRequiredMessage(chatId: Long): Result {
    log(chatId, "updating pinned message", LogLevel.Info)
    try {
        val day = LocalDate.now().dayOfWeek
        (storedSchedule[chatId] ?: return Result.Error).messages.forEach { message ->
            if (message.messageInfo.messageId == -1L) {
                return Result.MessageNotFound
            }
            // if we need to change pin state
            if (day == message.dayOfWeek && !message.messageInfo.pinState) {
                bot.pinChatMessage(chatId.toChatId(), message.messageInfo.messageId, true)
                message.messageInfo.pinState = true
            }

            if (day != message.dayOfWeek && message.messageInfo.pinState) {
                bot.unpinChatMessage(chatId.toChatId(), message.messageInfo.messageId)
                message.messageInfo.pinState = false
            }
        }
    } catch (e: TelegramApiError) {
        if (e.code == 400) {
            log(chatId, "exception caught \n$e", LogLevel.Debug)
            telegramApiErrorMap.onEach {
                if (e.description.contains(it.key)) return it.value
            }
            log(chatId, "unexpected telegram api error was thrown \n$e", LogLevel.Error)
            return Result.Error
        }
    }
    return Result.Success
}

/**
 * this is used for processing pin/unpin function
 */
val telegramApiErrorMap: Map<String, Result> = mapOf(
    "not enough rights" to Result.NotEnoughRight,
    "chat not found" to Result.ChatNotFound,
    "message to pin not found" to Result.MessageNotFound
)
