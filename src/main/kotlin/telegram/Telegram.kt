package telegram

import com.elbekd.bot.Bot
import com.elbekd.bot.model.toChatId
import com.elbekd.bot.types.Message
import data.*
import matchesWith
import russianName
import java.time.LocalDate

/**
 * it creates a telegram bot, using provided token
 */
val bot: Bot = Bot.createPolling(token)

/**
 * it displays all data in the chat like this:
 *
 *   Monday
 *  PE {в 13} (Ivan Ivanov)
 *  Math {в 1} (Ivan Nikolay)
 *  etc...
 * @param chatId id of telegram chat
 * @param shouldResendMessage we use it if a user does /output
 */
suspend fun UserSchedule.displayInChat(chatId: Long, shouldResendMessage: Boolean) {
    log(chatId, "outputting schedule data")

    // we do this backwards, so we don't output non-existing lessons, while keeping info about first ones
    this.messages.forEachIndexed { index, it ->
        var werePrevious = false
        var messageText = ""

        it.lessonInfo.reversed().forEach { info ->
            when (info.lesson) {
                "" -> if (werePrevious) messageText = "\n$messageText"

                else -> {
                    messageText = "${info.lesson} {в ${info.classroom}} (${info.teacher}) \n$messageText"
                    werePrevious = true
                }
            }
        }

        messageText = " ${it.dayOfWeek.russianName()} \n$messageText"

        if (shouldResendMessage && storedSchedule[chatId] != null && storedSchedule[chatId]!!.messages.size > 0 && storedSchedule[chatId]!!.messages.all {
                it.messageInfo.messageId != -1L
            }) {
            if (!storedSchedule[chatId]!!.matchesWith(this)) {
                try {
                    val id = bot.editMessageText(
                        chatId.toChatId(),
                        storedSchedule[chatId]!!.messages[index].messageInfo.messageId,
                        text = messageText
                    )
                    it.messageInfo = MessageInfo(id.messageId, false)

                } catch (e: Exception) {
                    sendErrorMessage(chatId)
                    log(chatId, "error ${storedSchedule[chatId]!!.messages} $e")
                }
            }
        } else {
            val id = sendMessage(chatId, messageText)
            it.messageInfo = MessageInfo(id, false)
        }
    }
    storedSchedule[chatId] = this
    storeConfigs(chatId, chosenClass[chatId]!!, chosenLink[chatId]!!, updateTime[chatId]!!, storedSchedule[chatId]!!)
}

/**
 * sends message in telegram chat
 * @param chatId id of telegram chat
 * @param text is a string we want to output
 */
suspend fun sendMessage(chatId: Long, text: String): Long {
    return try {
        bot.sendMessage(chatId.toChatId(), text).messageId
    } catch (e: Exception) {
        println("An exception has occurred while sending message")
        println(e.stackTraceToString())
        println("text is \n$text")
        -1L
    }
}

/**
 * sends message about error in telegram chat and to @LichnyiSvetM
 * @param chatId id of telegram chat
 */
suspend fun sendErrorMessage(chatId: Long) {
    try {
        bot.sendMessage(
            chatId.toChatId(), "Произошла какая-то ошибка, свяжитесь с создателем бота (@LichnyiSvetM)"
        ).messageId
    } catch (e: Exception) {
        println("An exception has occurred while sending message")
        println(e.stackTraceToString())
        println("text is \n$\"Произошла какая-то ошибка, свяжитесь с создателем бота (@LichnyiSvetM)\"")
    }
}

/**
 * sends message in telegram chat
 * @param message any message from telegram chat
 * @param text is a string we want to output
 */
suspend fun sendMessage(message: Message, text: String): Long {
    return sendMessage(message.chat.id, text)
}

/**
 * it is used to pin only schedule for the current day
 */
suspend fun processPin(chatId: Long) {
    val day = LocalDate.now().dayOfWeek
    storedSchedule[chatId]?.messages!!.forEach { message ->
        if (message.messageInfo.messageId != -1L) {
            if (day == message.dayOfWeek) {
                // if we need to change pin state
                if (!message.messageInfo.pinState) {
                    bot.pinChatMessage(chatId.toChatId(), message.messageInfo.messageId, true)
                    message.messageInfo.pinState = true
                }
            } else {
                // it shouldn't be pinned anymore
                if (message.messageInfo.pinState) {
                    bot.unpinChatMessage(chatId.toChatId(), message.messageInfo.messageId)
                    message.messageInfo.pinState = false
                }
            }
        } else sendErrorMessage(chatId)
    }
    previousDay[chatId] = day
}