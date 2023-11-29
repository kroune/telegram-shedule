import com.elbekd.bot.model.toChatId
import data.*
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.forEachIndexed
import org.jetbrains.kotlinx.dataframe.api.getColumn
import org.jetbrains.kotlinx.dataframe.io.readCSV
import org.jetbrains.kotlinx.dataframe.size
import telegram.*
import java.net.URI
import java.net.UnknownHostException
import java.time.DayOfWeek

/**
 * loads data from provided url and converts it to mutableList
 * @param chatId ID of telegram chat
 */
fun getScheduleData(chatId: Long): UserSchedule? {
    // schedule for a day
    lateinit var currentDay: Pair<DayOfWeek?, MutableList<LessonInfo>>
    val formattedData = mutableListOf<Message>()

    try {
        // we read our dataFrame here, we read it in csv
        @Suppress("SpellCheckingInspection") val link = URI.create("$DEFAULT_LINK/gviz/tq?tqx=out:csv").toURL()
        val data = DataFrame.readCSV(link)

        log(chatId, "starting data update", LogLevel.Info)
        // we iterate over first column, where lesson number is stored
        data.getColumnOrNull(1)?.forEachIndexed { index, element ->
            // week day
            data.getColumn(0)[index].let { dayElement ->
                if (!dayElement.empty()) {
                    if (index != 0) {
                        formattedData.add(
                            Message(currentDay.first, currentDay.second, MessageInfo(-1L, false))
                        )
                    }
                    // clears currentDay value
                    currentDay = Pair(getDay(dayElement.toString()), mutableListOf())
                }
            }
            // if we are the end of our dataFrame or row is empty
            if (index == data.size().nrow - 1 || element.removeNull()
                    .any { !it.isDigit() } || element.empty()
            ) return@forEachIndexed

            element.let {
                /*
                * it is located like
                *
                * subject teacher
                *         classroom
                */

                val classColumnIndex = data.getColumnIndex(chosenClass[chatId]!!)
                val subject = data.getColumn(classColumnIndex)[index].removeNull()

                if (subject.empty() || subject.isBlank()) {
                    currentDay.second.add(LessonInfo("", "", ""))
                } else {
                    val teacher = data.getColumn(classColumnIndex + 1)[index].removeNull()
                    val classroom = data.getColumn(classColumnIndex + 1)[index + 1].removeNull()
                    currentDay.second.add(LessonInfo(subject, teacher, classroom))
                }
            }
        }
    } catch (e: IllegalArgumentException) {
        sendAsyncMessage(chatId, "Не удалось обновить информацию, вы уверены, что ввели все данные правильно?")
        log(chatId, "Incorrect class name \n ${e.stackTrace}", LogLevel.Error)
        return UserSchedule(mutableListOf())
    } catch (e: IndexOutOfBoundsException) {
        sendAsyncMessage(chatId, "Не удалось обновить информацию, вы уверены, что ввели все данные правильно?")
        log(chatId, "Incorrect class name \n ${e.stackTrace}", LogLevel.Error)
        return UserSchedule(mutableListOf())
    } catch (e: UnknownHostException) {
        log(chatId, "Failed to connect \n ${e.stackTrace}", LogLevel.Info)
        return null
    }
    formattedData.add(
        Message(currentDay.first, currentDay.second, MessageInfo(-1L, false))
    )
    log(chatId, "formatted data - $formattedData", LogLevel.Debug)
    return UserSchedule(formattedData)
}

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

        message.lessonInfo.asReversed().forEach { info ->
            when (info.lesson) {
                "" -> if (werePrevious) messageText = "\n $messageText"

                else -> {
                    messageText = "${info.lesson} {в ${info.classroom}} (${info.teacher}) \n $messageText"
                    werePrevious = true
                }
            }
        }

        messageText = " ${message.dayOfWeek.russianName()} \n $messageText"

        if (!shouldResendMessage && storedSchedule[chatId] != null && storedSchedule[chatId]!!.messages.isNotEmpty() &&
            storedSchedule[chatId]!!.messages.all {
                it.messageInfo.messageId != -1L
            }
        ) {
            if (!storedSchedule[chatId]!!.matchesWith(this)) {
                try {
                    val id = bot.editMessageText(
                        chatId.toChatId(),
                        storedSchedule[chatId]!!.messages[index].messageInfo.messageId,
                        text = messageText
                    )
                    message.messageInfo = MessageInfo(id.messageId, false)

                } catch (e: Exception) {
                    log(chatId, "error ${storedSchedule[chatId]!!.messages} ${e.stackTrace}", LogLevel.Error)
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
 * this function processes schedule pinging and handle errors
 * @param chatId ID of telegram chat
 */
suspend fun processSchedulePinning(chatId: Long) {
    when (pinRequiredMessage(chatId)) {
        Result.NotEnoughRight -> {
            log(chatId, "not enough rights", LogLevel.Info)
            if (!pinErrorShown[chatId]!!) {
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
            log(chatId, "successfully updated pinned messages", LogLevel.Debug)
            if (pinErrorShown[chatId]!!) {
                pinErrorShown[chatId] = false
            }
            storeConfigs(chatId)
        }
    }
}
