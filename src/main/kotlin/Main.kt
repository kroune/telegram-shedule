import com.elbekd.bot.Bot
import com.elbekd.bot.model.toChatId
import com.elbekd.bot.types.Message
import kotlinx.coroutines.*
import logger.loadData
import logger.log
import logger.storeConfigs
import java.time.DayOfWeek

/**
 * token is generated per bot, should be deleted, when uploading somewhere
 */
@Suppress("SpellCheckingInspection")
const val token: String = ""

/**
 * default schedule link
 */
const val defaultLink: String = "https://docs.google.com/spreadsheets/d/1L9UjNOZx4p4VER11SCyU97M07QnfWsZWwldAAOR0gtM"

/**
 * it is used to check if bot was initialized
 */
val initializedBot: MutableSet<Long> = mutableSetOf()

/**
 * class name (10Д, 8А, 9М, etc...)
 */
val chosenClass: MutableMap<Long, String> = mutableMapOf()

/**
 * google forms link
 */
val chosenLink: MutableMap<Long, String> = mutableMapOf()

/**
 * it is used for launching data updates
 */
val myCoroutine: CoroutineScope = CoroutineScope(Dispatchers.IO)

/**
 * time it waits (hours, minutes), before updating data, note that too small values might lead to an ip ban
 */
val updateTime: MutableMap<Long, Pair<Int, Int>> = mutableMapOf()

/**
 * it is used to check if bot is running
 */
val updateJob: MutableMap<Long, Job?> = mutableMapOf()

/**
 * it stores data for every class in schedule
 */
val storedSchedule: MutableMap<Long, MutableList<Triple<DayOfWeek?, MutableList<Triple<String, String, String>>, Long>>> =
    mutableMapOf()

/**
 * it creates a telegram bot, using provided token
 */
val bot: Bot = Bot.createPolling(token)

/**
 * here we initialize our commands and that bot
 */
suspend fun main() {
    loadData()
    buildRunChain()
    buildOutputChain()
    buildKillChain()
    buildUpdateChain()
    buildConfigureChain()
    bot.start()
}

/**
 * initialize default values for a new chat
 * @param chatId id of telegram chat
 */
suspend fun initializeChatValues(chatId: Long, className: String) {
    initializedBot.add(chatId)
    chosenClass[chatId] = className
    chosenLink[chatId] = defaultLink
    updateTime[chatId] = Pair(2, 0)
    log(chatId, "initializing variables")
    storeConfigs(chatId, className, defaultLink, Pair(0, 30), storedSchedule[chatId])
    launchScheduleUpdateCoroutine(chatId)
}

/**
 * sends message in telegram chat
 * @param chatId id of telegram chat
 * @param text is a string we want to output
 */
suspend fun sendMessage(chatId: Long, text: String): Long {
    return try {
        bot.sendMessage(chatId.toChatId(), text).messageId
    } catch (e: Exception) {
        println("An exception has occurred while sending message")
        println(e.stackTraceToString())
        println("text is \n$text")
        -1
    }
}

/**
 * sends message in telegram chat
 * @param message any message from telegram chat
 * @param text is a string we want to output
 */
suspend fun sendMessage(message: Message, text: String): Long {
    return sendMessage(message.chat.id, text)
}

fun MutableList<Triple<DayOfWeek?, MutableList<Triple<String, String, String>>, Long>>.matchesWith(compare: MutableList<Triple<DayOfWeek?, MutableList<Triple<String, String, String>>, Long>>?): Boolean {
    if (compare == null)
        return false
    this.forEachIndexed { index, triple ->
        if (triple.first != compare[index].first || triple.second != compare[index].second)
            return false
    }
    return true
}

/**
 * this checks if schedule has changed every 2 hours by default
 * @param chatId id of telegram chat
 */
suspend fun launchScheduleUpdateCoroutine(chatId: Long) {
    updateJob[chatId] = myCoroutine.launch {
        try {
            while (true) {
                log(chatId, "coroutine delay has passed")
                getScheduleData(chatId).let {
                    if (!it.matchesWith(storedSchedule[chatId])) {
                        it.displayInChat(chatId, true)
                        sendMessage(
                            chatId,
                            "Похоже расписание обновилось, если это не так, свяжитесь с создателем бота (@LichnyiSvetM)"
                        )
                    }
                }
                // 1000L = 1 second
                delay(1000L * (updateTime[chatId]!!.first * 3600 + updateTime[chatId]!!.second * 60))
            }
        } catch (e: CancellationException) {
            log(chatId, "Cancellation exception caught, this is expected")
        } catch (e: Exception) {
            println(e.cause)
            sendMessage(chatId, "Произошла какая-то ошибка, свяжитесь с создателем бота (@LichnyiSvetM)")
            log(chatId, e.stackTraceToString())
        }
    }
}
