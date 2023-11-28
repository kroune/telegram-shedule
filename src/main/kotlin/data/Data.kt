package data

import empty
import getDay
import kotlinx.coroutines.Job
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.forEachIndexed
import org.jetbrains.kotlinx.dataframe.api.getColumn
import org.jetbrains.kotlinx.dataframe.io.readCSV
import org.jetbrains.kotlinx.dataframe.size
import removeNull
import scheduleUpdateCoroutine
import telegram.sendAsyncMessage
import java.io.File
import java.net.URI
import java.net.UnknownHostException
import java.time.DayOfWeek
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * token is generated per bot, should be deleted, when uploading somewhere
 */
@Suppress("SpellCheckingInspection", "RedundantSuppression")
const val TOKEN: String = ""

/**
 * default schedule link
 */
const val DEFAULT_LINK: String = "https://docs.google.com/spreadsheets/d/1L9UjNOZx4p4VER11SCyU97M07QnfWsZWwldAAOR0gtM"

/**
 * it is used to make sure user did everything correctly
 */
val listOfClasses: Collection<String> = listOf(
    "2А",
    "2Б",
    "2В",
    "2Г",
    "3А",
    "3Б",
    "3В",
    "3Г",
    "4А",
    "4Б",
    "4В",
    "4Г",
    "5А",
    "5Б",
    "5В",
    "5Г",
    "5И",
    "5П",
    "6А",
    "6Б",
    "6В",
    "6Г",
    "6П",
    "7В",
    "7О",
    "7П",
    "7А",
    "7Б",
    "7Г",
    "8А",
    "8Б",
    "8В",
    "8Г",
    "8Д",
    "8М",
    "8Е",
    "8У",
    "8Ф",
    "8Я",
    "9А",
    "9В",
    "9Г",
    "9Д",
    "9М",
    "9Е",
    "9П",
    "9У",
    "9Ф",
    "9Х",
    "9Я",
    "10Б",
    "10В",
    "10Г",
    "10Д",
    "10М",
    "10Е",
    "10И",
    "10С",
    "10У",
    "10Ф",
    "10Я",
    "11А",
    "11В",
    "11Г",
    "11Д",
    "11М",
    "11Е",
    "11И",
    "11У",
    "11Ф",
    "11Я"
)

/**
 * it is used to check if bot was initialized
 */
val initializedBot: MutableSet<Long> = mutableSetOf()

/**
 * class name (10Д, 8А, 9М, etc...)
 */
val chosenClass: MutableMap<Long, String> = mutableMapOf()

/**
 * time it waits, before updating data, note that too small values might lead to an ip ban
 */
val updateTime: Duration = 30.minutes

/**
 * it is used to check if bot is running
 */
val updateJob: MutableMap<Long, Job?> = mutableMapOf()

/**
 * it stores data for every class in schedule
 */
val storedSchedule: MutableMap<Long, UserSchedule> = mutableMapOf()

/**
 * it is used not to spam chat with pinning errors and at the same time notifying about permission settings
 */
val pinErrorShown: MutableMap<Long, Boolean> = mutableMapOf()

/**
 * it is used to store data for every chat, so they don't have to rerun bot again
 * @param className - class name
 * @param schedule - stored schedule
 * @param pinErrorShown - used for people, who don't have enough skills to give a pin permission for a bot
 */
@Serializable
data class ConfigData(
    val className: String, val schedule: UserSchedule?, val pinErrorShown: Boolean
)

/**
 * it is used to store lesson info
 * @param lesson - lesson name
 * @param teacher - teacher name
 * @param classroom - where class will be
 */
@Serializable
class LessonInfo(
    val lesson: String, val teacher: String, val classroom: String
)

/**
 * it is used to store info about messages in chat
 * @param dayOfWeek - schedule's day of week
 * @param lessonInfo - list of all lessons
 * @param messageInfo - used to store info, needed for telegram part (pinning messages/editing them)
 */
@Serializable
class Message(
    val dayOfWeek: DayOfWeek?, val lessonInfo: MutableList<LessonInfo>, var messageInfo: MessageInfo
)

/**
 * it is used not to deal with 2 mutableList and to make things clearer
 * @param messages - messages in this user's chat
 */
@Serializable
class UserSchedule(
    val messages: MutableList<Message>
)

/**
 * it is used to store important info about messages (for telegram part)
 * @param messageId - id of the message we sent (-1L before it happens)
 * @param pinState - represents if a message is pinned or not
 */
@Serializable
class MessageInfo(
    val messageId: Long, var pinState: Boolean
)

/**
 * deletes data for user
 */
fun deleteData(chatId: Long) {
    val file = File("data/$chatId.json")
    if (file.exists()) {
        if (!File("data/outdated/").exists()) File("data/outdated/").mkdir()
        file.copyTo(File("data/outdated/$chatId.json"))
        file.delete()
    }
}

/**
 * stores config data in data/ folder
 */
fun storeConfigs(chatId: Long) {
    val configData = ConfigData(chosenClass[chatId]!!, storedSchedule[chatId], pinErrorShown[chatId]!!)
    val encodedConfigData = Json.encodeToString(configData)
    log(chatId, configData.toString(), LogLevel.Debug)
    val file = File("data/$chatId.json")
    if (!file.exists()) {
        file.createNewFile()
    }
    file.writeText(encodedConfigData)
}

/**
 * loads data on program startup
 */
fun loadData() {
    if (!File("data/").exists()) {
        File("data/").mkdir()
    }
    File("data/").walk().forEach {
        if (it.isDirectory || it.path.contains("outdated")) return@forEach
        val textFile = it.bufferedReader().use { text ->
            text.readText()
        }
        val configData = Json.decodeFromString<ConfigData>(textFile)
        val chatId = it.name.dropLast(5).toLong()
        chosenClass[chatId] = configData.className
        pinErrorShown[chatId] = configData.pinErrorShown
        initializedBot.add(chatId)
        configData.schedule?.let { schedule ->
            storedSchedule[chatId] = schedule
        }
        scheduleUpdateCoroutine(chatId)
        log(chatId, configData.toString(), LogLevel.Debug)
    }
}

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
        log(chatId, "Incorrect class name \n$e", LogLevel.Error)
        return UserSchedule(mutableListOf())
    } catch (e: IndexOutOfBoundsException) {
        sendAsyncMessage(chatId, "Не удалось обновить информацию, вы уверены, что ввели все данные правильно?")
        log(chatId, "Incorrect class name \n$e", LogLevel.Error)
        return UserSchedule(mutableListOf())
    } catch (e: UnknownHostException) {
        log(chatId, "Failed to connect \n $e", LogLevel.Info)
        return null
    }
    formattedData.add(
        Message(currentDay.first, currentDay.second, MessageInfo(-1L, false))
    )
    log(chatId, "formatted data - $formattedData", LogLevel.Debug)
    return UserSchedule(formattedData)
}
