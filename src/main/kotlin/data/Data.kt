package data

import IS_TEST
import kotlinx.coroutines.Job
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import scheduleUpdateCoroutine
import java.io.File
import java.time.DayOfWeek
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes


/**
 * time it waits, before updating data, note that too small values might lead to an ip ban
 */
val updateTime: Duration = 30.minutes

/**
 * it is used to store all configs
 */
object Config {
    /**
     * it is used to check if bot is running
     */
    val updateJob: MutableMap<Long, Job?> = mutableMapOf()

    /**
     * it is used for storing current configs
     */
    val configs: MutableMap<Long, ConfigData> = mutableMapOf()

    /**
     * it is used to check if a user has proper data
     * @param id ID of telegram chat
     */
    fun isNotNull(id: Long): Boolean = configs.contains(id)

    /**
     * it checks if bot was initialized and stops if it wasn't
     * @param id ID of telegram chat
     */
    fun hasStartedBot(id: Long): Boolean = isNotNull(id) && configs[id]!!.initializedBot
}

/**
 * it is used to store data for every chat, so they don't have to rerun bot again
 * @param className - class name
 * @param schedule - stored schedule
 * @param pinErrorShown - used for people, who don't have enough skills to give a pin permission for a bot
 * @param notifyAboutChanges - used for disabling/enabling notification on schedule changes
 * @param shouldRePin - used for disabling/enabling pinning current schedule
 * @param initializedBot - used to check if bot has been initialized
 */
@Serializable
data class ConfigData(
    var className: String,
    var schedule: UserSchedule,
    var pinErrorShown: Boolean,
    var notifyAboutChanges: Boolean,
    var shouldRePin: Boolean,
    val initializedBot: Boolean
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
) {
    /**
     * it is used to check if all messages have valid id
     */
    fun hasMessageId(): Boolean {
        return messages.all { it.messageInfo.messageId != -1L }
    }
}

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
 * this is used to understand where we need to store config, this is needed to prevent file conflicts
 */
val dataDirectory: String = ".data/${if (IS_TEST) "test/" else "production/"}"

/**
 * deletes data for user
 * @param chatId ID of telegram chat
 */
fun deleteData(chatId: Long) {
    val file = File("$dataDirectory$chatId.json")
    if (file.exists()) {
        if (!File("${dataDirectory}outdated/").exists()) File("${dataDirectory}outdated/").mkdir()
        file.copyTo(File("${dataDirectory}outdated/$chatId.json"), overwrite = true)
        file.delete()
    }
}

/**
 * it is used if json config file is corrupted or doesn't have new fields
 * @param chatId ID of telegram chat
 */
fun invalidateData(chatId: Long) {
    val file = File("$dataDirectory$chatId.json")
    if (file.exists()) {
        if (!File("${dataDirectory}invalid/").exists()) File("${dataDirectory}invalid/").mkdir()
        file.copyTo(File("${dataDirectory}invalid/$chatId.json"), overwrite = true)
        file.delete()
    }
}

/**
 * stores config data in data/ folder
 * @param chatId ID of telegram chat
 */
fun storeConfigs(chatId: Long) {
    try {
        require(Config.isNotNull(chatId))
        Config.configs[chatId].let {
            debug(chatId, it.toString())
            val file = File("$dataDirectory$chatId.json")
            if (!file.exists()) {
                file.createNewFile()
            }
            file.writeText(Json.encodeToString(it))
        }
    } catch (e: IllegalArgumentException) {
        error(chatId, "an exception occurred when storing configs \n ${e.stackTrace}")
    }
}

/**
 * loads data on program startup
 */
@OptIn(ExperimentalSerializationApi::class)
fun loadData() {
    if (!File(dataDirectory).exists()) {
        File(dataDirectory).mkdir()
    }
    File(dataDirectory).listFiles()!!.filter { it.isFile }.forEach {
        try {
            val textFile = it.bufferedReader().use { text ->
                text.readText()
            }
            val configData = Json.decodeFromString<ConfigData>(textFile)
            val chatId = it.name.dropLast(5).toLong()
            Config.configs[chatId] = configData
            scheduleUpdateCoroutine(chatId)
            info(chatId, "loading data from $it")
            debug(chatId, configData.toString())
        } catch (e: MissingFieldException) {
            println("an exception occurred when loading data ${e.stackTraceToString()}")
            if (IS_TEST) invalidateData(it.name.dropLast(5).toLong())
        } catch (e: SerializationException) {
            println("an exception occurred when loading data ${e.stackTraceToString()}")
            if (IS_TEST) invalidateData(it.name.dropLast(5).toLong())
        }
    }
}
