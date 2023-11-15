package data

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * it is used to log debug info
 */
// TODO: add proper log warning level
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