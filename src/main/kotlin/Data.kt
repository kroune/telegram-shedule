import com.elbekd.bot.model.TelegramApiError
import com.elbekd.bot.model.toChatId
import logger.log
import logger.storeConfigs
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.forEachIndexed
import org.jetbrains.kotlinx.dataframe.api.getColumn
import org.jetbrains.kotlinx.dataframe.io.readCSV
import org.jetbrains.kotlinx.dataframe.size
import java.net.URL
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.*


/**
 * loads data from provided url and converts it to mutableList
 * @param chatId id of telegram chat
 */
suspend fun getScheduleData(chatId: Long): MutableList<Triple<String, MutableList<Triple<String, String, String>>, Long>> {
    @Suppress("SpellCheckingInspection") val data = DataFrame.readCSV(URL("${chosenLink[chatId]}/gviz/tq?tqx=out:csv"))

    // schedule for a day
    lateinit var currentDay: Pair<String, MutableList<Triple<String, String, String>>>
    val formattedData = mutableListOf<Triple<String, MutableList<Triple<String, String, String>>, Long>>()

    log(chatId, "starting data update")
    try {
        data.getColumnOrNull(1)?.forEachIndexed { index, element ->
            data.getColumnOrNull(0)?.let { day ->
                day[index].let { dayElement ->
                    if (!dayElement.empty()) {
                        if (index != 0) formattedData.add(Triple(currentDay.first, currentDay.second, -1L))
                        // clears currentDay value
                        currentDay = Pair(dayElement!!.toString(), mutableListOf())
                    }
                }
            }
            if (index == data.size().nrow - 1 || element.removeNull()
                    .any { !it.isDigit() } || element.empty()
            ) return@forEachIndexed

            element.let {
                val classColumnIndex = data.getColumnIndex(chosenClass[chatId]!!)
                /*
                * it is located like
                *
                * subject teacher
                *        classroom
                */
                val subject = data.getColumn(classColumnIndex)[index].removeNull()
                if (subject.empty() || subject.isBlank()) currentDay.second.add(
                    Triple(
                        "", "", ""
                    )
                )
                else {
                    val teacher = data.getColumn(classColumnIndex + 1)[index].removeNull()
                    val classroom = data.getColumn(classColumnIndex + 1)[index + 1].removeNull()
                    currentDay.second.add(
                        Triple(
                            subject, teacher, classroom
                        )
                    )
                }
            }
        }
    } catch (e: IllegalArgumentException) {
        sendMessage(chatId, "Не удалось обновить информацию, вы уверены, что ввели все данные правильно?")
        log(chatId, "Incorrect class name")
        formattedData.clear()
        return formattedData
    }
    formattedData.add(Triple(currentDay.first, currentDay.second, -1L))
    return formattedData
}

/**
 * it displays all data in the chat like this:
 *
 *   Monday
 *  PE {в 13} (Ivan Ivanov)
 *  Math {в 1} (Ivan Nikolay)
 *  etc...
 * @param chatId id of telegram chat
 * @param shouldResendMessage we use it if user does /output
 */
suspend fun MutableList<Triple<String, MutableList<Triple<String, String, String>>, Long>>.displayInChat(
    chatId: Long, shouldResendMessage: Boolean
) {
    val data: MutableList<Triple<String, MutableList<Triple<String, String, String>>, Long>> = mutableListOf()
    log(chatId, "outputting schedule data")
    // we do this backwards, so we don't output non-existing lessons, while keeping info about first ones
    this.forEachIndexed { index, it ->
        var werePrevious = false
        var str = ""
        it.second.reversed().forEach { (lesson, teacher, classroom) ->
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
        str = " ${it.first} \n" + str
        if (shouldResendMessage && storedSchedule[chatId]?.all { it.third != -1L } == true) {

            try {
                val id = bot.editMessageText(chatId.toChatId(), storedSchedule[chatId]!![index].third, text = str)
                data.add(Triple(it.first, it.second, id.messageId))
            } catch (e: Exception) {
                log(chatId, "new text matches previous one $e")
            }
        } else {
            processPin()
            val id = sendMessage(chatId, str)
            data.add(Triple(it.first, it.second, id))
            bot.pinChatMessage(chatId.toChatId(), id)
        }
    }
    storedSchedule[chatId] = data
    storeConfigs(
        chatId, chosenClass[chatId]!!, chosenLink[chatId]!!, updateTime[chatId]!!, storedSchedule[chatId]!!
    )
}

fun processPin() {
    val day = LocalDate.now().getDayOfWeek().name
    println(" C DATE is  $day")
}

fun getDate(idk: String): DayOfWeek? {
    if (idk.contains("Понед")) {
        return DayOfWeek.MONDAY
    }
    return null
}

/**
 * it is used to prevent "null" s, which appear if a line is missing from going to the chat
 */
fun Any?.removeNull(): String {
    return if (!this.empty()) this.toString()
    else ""
}

/**
 * it is used to check if data, we read is empty
 */
fun Any?.empty(): Boolean {
    return this.toString() == "null"
}