package data

import kotlinx.coroutines.Job
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import scheduleUpdateCoroutine
import java.io.File
import java.time.DayOfWeek
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * token is generated per bot, should be deleted, when uploading somewhere
 */
@Suppress("SpellCheckingInspection", "RedundantSuppression")
const val TOKEN: String = ""

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
    val className: String, val schedule: UserSchedule, val pinErrorShown: Boolean
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
    val configData = ConfigData(chosenClass[chatId]!!, storedSchedule[chatId]!!, pinErrorShown[chatId]!!)
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
        configData.schedule.let { schedule ->
            storedSchedule[chatId] = schedule
        }
        scheduleUpdateCoroutine(chatId)
        log(chatId, configData.toString(), LogLevel.Debug)
    }
}
