package data

import telegram.sendAsyncMessage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * it is used to log debug info
 */
//TODO: add ban system
fun log(chatId: Long, text: String, warningLevel: LogLevel) {
    val currentDate = SimpleDateFormat("dd/M/yyyy hh:mm:ss").format(Date())
    try {
        if (!File("logs/").exists()) {
            File("logs/").mkdir()
        }
        val file = File("logs/${chatId}.log")
        if (!file.exists()) file.createNewFile()
        file.appendText("LOG: $currentDate $text\n")
    } catch (e: Exception) {
        println("an Exception occurred, during logging \n${e.stackTraceToString()}")
    }
    when (warningLevel) {
        LogLevel.Error -> {
            try {
                sendAsyncMessage(
                    chatId, "Произошла какая-то ошибка, свяжитесь с создателем бота (@LichnyiSvetM)"
                )
                sendAsyncMessage((1376927355).toLong(), "Во время работы бота произошла ошибка $chatId\n$text")
                println("(id - $chatId) $currentDate $text")
            } catch (e1: Exception) {
                println("An exception has occurred while sending message")
                println(e1.stackTraceToString())
                println("text is \n$\"Произошла какая-то ошибка, свяжитесь с создателем бота (@LichnyiSvetM)'\n$e1\"")
            }
        }

        LogLevel.Info -> {
            println("(id - $chatId) $currentDate $text")
        }

        else -> {
        }
    }
}

/**
 * it is used to print only necessary messages
 */
enum class LogLevel {
    /**
     * used for debug
     */
    Debug,

    /**
     * we print useful info using it
     */
    Info,

    /**
     * Something is definitely wrong
     */
    Error
}