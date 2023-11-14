package telegram

import com.elbekd.bot.Bot
import com.elbekd.bot.model.toChatId
import com.elbekd.bot.types.Message
import data.*
import java.time.DayOfWeek
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
suspend fun MutableList<Triple<DayOfWeek?, MutableList<Triple<String, String, String>>, Pair<Long, Boolean>>>.displayInChat(
    chatId: Long, shouldResendMessage: Boolean
) {
    val data: MutableList<Triple<DayOfWeek?, MutableList<Triple<String, String, String>>, Pair<Long, Boolean>>> =
        mutableListOf()
    log(chatId, "outputting schedule data")
    // we do this backwards, so we don't output non-existing lessons, while keeping info about first ones
    this.forEachIndexed { index, (first, second, _) ->
        var werePrevious = false
        var str = ""
        second.reversed().forEach { (lesson, teacher, classroom) ->
            when (lesson) {
                "" -> {
                    if (werePrevious) str = "\n$str"
                }

                else -> {
                    str = "$lesson {в $classroom} ($teacher) \n$str"
                    werePrevious = true
                }
            }
        }
        str = " $first \n$str"
        if (shouldResendMessage && storedSchedule[chatId] != null && storedSchedule[chatId]!!.size > 0 && storedSchedule[chatId]!!.all { it.third.first != -1L }) {
            try {
                val id = bot.editMessageText(chatId.toChatId(), storedSchedule[chatId]!![index].third.first, text = str)
                data.add(Triple(first, second, Pair(id.messageId, false)))
            } catch (e: Exception) {
                sendMessage(chatId, "Произошла какая-то ошибка, свяжитесь с создателем бота (@LichnyiSvetM)")
                log(chatId, "error ${storedSchedule[chatId]!!.size} $e")
            }
        } else {
            val id = sendMessage(chatId, str)
            data.add(Triple(first, second, Pair(id, false)))
        }
    }
    storedSchedule[chatId] = data
    storeConfigs(
        chatId,
        chosenClass[chatId]!!,
        chosenLink[chatId]!!,
        updateTime[chatId]!!,
        storedSchedule[chatId]!!
    )
    processPin(chatId)
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
        -1
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

suspend fun processPin(chatId: Long) {
    val day = LocalDate.now().dayOfWeek
    if (day != previousDay[chatId]) {
        day.let {
            storedSchedule[chatId]?.forEachIndexed { index, (first, _, third) ->
                if (third.first != -1L) {
                    if (first == it) {
                        // if we need to change pin state
                        if (!third.second) {
                            bot.pinChatMessage(chatId.toChatId(), third.first)
                            // TODO: change stored data info
                        }
                    } else {
                        // it shouldn't be pinned anymore
                        if (third.second) {
                            bot.unpinChatMessage(chatId.toChatId(), third.first)
                        }
                    }
                }
                // smth wrong
            }
        }
    }
}