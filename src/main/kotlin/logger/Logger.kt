package logger

import chosenClass
import chosenLink
import kotlinx.coroutines.launch
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import launchScheduleUpdateCoroutine
import myCoroutine
import updateJob
import updateTime
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * it is used to store data for every chat, so they don't have to rerun bot again
 * @param className - class name
 * @param link - link with schedule
 * @param time - update delay time
 */
@Serializable
data class ConfigData(val className: String, val link: String, val time: Pair<Int, Int>)

/**
 * it is used to log debug info
 */
fun log(chatId: Long, text: String) {
    val currentDate = SimpleDateFormat("dd/M/yyyy hh:mm:ss").format(Date())
    try {
        val file = File("${chatId}.log")
        if (!file.exists()) {
            file.createNewFile()
            file.writeText(chosenClass[chatId]!!)
        }
        file.writeText("LOGGING: $currentDate $text")
    } catch (e: Exception) {
        println("an Exception occurred, during logging \n$e")
    }
}

/**
 * stores config data in data/ folder
 */
fun storeConfigs(chatId: Long, className: String, link: String, data: Pair<Int, Int>) {
    val configData = ConfigData(className, link, data)
    val encodedConfigData = Json.encodeToString(configData)
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
    File("data/").walk().forEach {
        if (it.isDirectory)
            return@forEach
        println(it.name)
        val textFile = it.bufferedReader().use { it1 -> it1.readText() }
        val obj = Json.decodeFromString<ConfigData>(textFile)
        val chatId = it.name.dropLast(5).toLong()
        chosenClass[chatId] = obj.className
        chosenLink[chatId] = obj.link
        updateTime[chatId] = obj.time
        updateJob[chatId] = myCoroutine.launch {
            launchScheduleUpdateCoroutine(chatId)
        }
        println(obj)
    }
}