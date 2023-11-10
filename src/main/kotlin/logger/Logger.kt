package logger

import chosenClass
import chosenLink
import initializedBot
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import launchScheduleUpdateCoroutine
import storedSchedule
import updateTime
import java.io.File
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.util.*

/**
 * it is used to store data for every chat, so they don't have to rerun bot again
 * @param className - class name
 * @param link - link with schedule
 * @param time - update delay time
 * @param schedule - stored schedule
 */
@Serializable
data class ConfigData(
    val className: String,
    val link: String,
    val time: Pair<Int, Int>,
    val schedule: MutableList<Triple<DayOfWeek?, MutableList<Triple<String, String, String>>, Long>>?
)

/**
 * it is used to log debug info
 */
fun log(chatId: Long, text: String) {
    val currentDate = SimpleDateFormat("dd/M/yyyy hh:mm:ss").format(Date())
    println("(id - $chatId) $currentDate $text")
    try {
        if (!File("logs/").exists()) {
            File("logs/").mkdir()
        }
        val file = File("logs/${chatId}.log")
        if (!file.exists())
            file.createNewFile()
        file.appendText("LOG: $currentDate $text\n")
    } catch (e: Exception) {
        println("an Exception occurred, during logging \n${e.stackTraceToString()}")
    }
}

/**
 * stores config data in data/ folder
 */
fun storeConfigs(
    chatId: Long,
    className: String,
    link: String,
    data: Pair<Int, Int>,
    schedule: MutableList<Triple<DayOfWeek?, MutableList<Triple<String, String, String>>, Long>>?
) {
    val configData = ConfigData(className, link, data, schedule)
    val encodedConfigData = Json.encodeToString(configData)
    val file = File("data/$chatId.json")
    if (!file.exists())
        file.createNewFile()
    file.writeText(encodedConfigData)
}

/**
 * loads data on program startup
 */
suspend fun loadData() {
    if (!File("data/").exists()) {
        File("data/").mkdir()
    }
    File("data/").walk().forEach {
        if (it.isDirectory) return@forEach
        val textFile = it.bufferedReader().use { text ->
            text.readText()
        }
        val configData = Json.decodeFromString<ConfigData>(textFile)
        val chatId = it.name.dropLast(5).toLong()
        chosenClass[chatId] = configData.className
        chosenLink[chatId] = configData.link
        updateTime[chatId] = configData.time
        initializedBot.add(chatId)
        configData.schedule?.let { schedule ->
            storedSchedule[chatId] = schedule
        }
        launchScheduleUpdateCoroutine(chatId)
        println(configData)
    }
}