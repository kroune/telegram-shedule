import com.elbekd.bot.model.toChatId
import data.*
import data.Config.configs
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
        @Suppress("SpellCheckingInspection")
        val link = URI.create("$DEFAULT_LINK/gviz/tq?tqx=out:csv&gid=727818446").toURL()
        val data = DataFrame.readCSV(link)

        info(chatId, "starting data update")
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
            if (index == data.size().nrow - 1 || element.removeNull().isBlank()) return@forEachIndexed

            element.let {
                /*
                * it is located like
                *
                * subject teacher
                *         classroom
                */

                val upperCase = data.getColumnIndex(configs[chatId]!!.className.uppercase()) != -1
                val classColumnIndex = data.getColumnIndex(
                    if (upperCase) configs[chatId]!!.className
                    else configs[chatId]!!.className.lowercase()
                )
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
        error(chatId, "Incorrect class name \n ${e.stackTraceToString()}")
        return UserSchedule(mutableListOf())
    } catch (e: IndexOutOfBoundsException) {
        sendAsyncMessage(chatId, "Не удалось обновить информацию, вы уверены, что ввели все данные правильно?")
        error(chatId, "Incorrect class name \n ${e.stackTraceToString()}")
        return UserSchedule(mutableListOf())
    } catch (e: UnknownHostException) {
        info(chatId, "Failed to connect \n ${e.stackTraceToString()}")
        return null
    }
    formattedData.add(
        Message(currentDay.first, currentDay.second, MessageInfo(-1L, false))
    )
    debug(chatId, "formatted data - $formattedData")
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
    info(chatId, "outputting schedule data")
    require(Config.isNotNull(chatId))
    val configData = configs[chatId]!!
    // we do this backwards, so we don't output non-existing lessons, while keeping info about first ones
    this@displayInChat.messages.forEachIndexed { index, message ->
        var werePrevious = false
        var messageText = ""

        message.lessonInfo.asReversed().forEach { info ->
            if (info.lesson == "") {
                if (werePrevious) messageText = "\n $messageText"
            } else {
                val classroom = if (info.classroom != "") "{в ${info.classroom}}" else ""
                val teacher = if (info.teacher != "") "(${info.teacher})" else ""
                messageText = "${info.lesson} $classroom $teacher \n $messageText"
                werePrevious = true
            }
        }

        messageText = " ${message.dayOfWeek.russianName()} \n $messageText"

        if (!shouldResendMessage && configData.schedule.messages.isNotEmpty() && configData.schedule.hasMessageId()) {
            if (!configData.schedule.matchesWith(this)) {
                try {
                    val id = bot.editMessageText(
                        chatId.toChatId(), configData.schedule.messages[index].messageInfo.messageId, text = messageText
                    )
                    message.messageInfo = MessageInfo(id.messageId, false)

                } catch (e: Exception) {
                    error(chatId, "error ${configData.schedule.messages} ${e.stackTraceToString()}")
                }
            }
        } else {
            val id = sendMessage(chatId, messageText)
            message.messageInfo = MessageInfo(id, false)
        }
    }
    configData.schedule = this
    storeConfigs(chatId)
}

/**
 * this function processes schedule pinging and handle errors
 * @param chatId ID of telegram chat
 */
suspend fun processSchedulePinning(chatId: Long) {
    pinRequiredMessage(chatId).let {
        when (it.first) {
            Result.NotEnoughRight -> {
                info(chatId, "not enough rights")
                if (!configs[chatId]!!.pinErrorShown) {
                    debug(chatId, "outputting rights warning")
                    configs[chatId]!!.pinErrorShown = true
                    sendMessage(chatId, "не достаточно прав для закрепления сообщения")
                    storeConfigs(chatId)
                }
            }

            Result.ChatNotFound -> {
                info(chatId, "chat with id $chatId was deleted")
                deleteData(chatId)
            }

            Result.Error -> {
                error(chatId, "an error has occurred")
                //TODO: add error counter and retry
            }

            Result.MessageNotFound -> {
            }

            Result.Success -> {
                debug(chatId, "successfully updated pinned messages")
                if (configs[chatId]!!.pinErrorShown && it.second) {
                    configs[chatId]!!.pinErrorShown = false
                }
                storeConfigs(chatId)
            }
        }
    }
}
