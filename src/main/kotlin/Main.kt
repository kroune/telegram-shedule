import com.elbekd.bot.Bot
import com.elbekd.bot.model.toChatId
import com.elbekd.bot.types.Message
import kotlinx.coroutines.*
import logger.loadData
import logger.log
import logger.storeConfigs

/**
 * token is generated per bot, should be deleted, when uploading somewhre
 */
@Suppress("SpellCheckingInspection")
const val token: String = "6722149681:AAFzavXlMrrYfimFunPC_-4fwVMnGWxc6pE"

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
val storedData: MutableMap<Long, MutableList<Pair<String, MutableList<Triple<String, String, String>>>>> =
    mutableMapOf()

/**
 * it creates a telegram bot, using provided token
 */
val bot: Bot = Bot.createPolling(token)

/**
 * here we initialize our commands and that bot
 */
fun main() {
    loadData()
    buildRunChain()
    buildUpdateChain()
    buildConfigureChain()
    bot.start()
}

/**
 * initialize default values for a new chat
 */
fun initializeChatValues(messageId: Long, className: String) {
    initializedBot.add(messageId)
    chosenClass[messageId] = className
    chosenLink[messageId] = defaultLink
    updateTime[messageId] = Pair(2, 0)
    log(messageId, "initializing variables")
    storeConfigs(messageId, className, defaultLink, Pair(2, 0))
    try {
        updateJob[messageId] = myCoroutine.launch {
            launchScheduleUpdateCoroutine(messageId)
        }
    } catch (e: Exception) {
        //crashReport(e)
    }
}

/**
 * this way it takes less space
 * @param text is a string we want to output
 */
suspend fun sendMessage(messageId: Long, text: String) {
    bot.sendMessage(messageId.toChatId(), text)
}

/**
 * this way it takes less space
 * @param text is a string we want to output
 */
suspend fun sendMessage(message: Message, text: String) {
    bot.sendMessage(message.chat.id.toChatId(), text)
}

/**
 * this checks if schedule has changed every 2 hours by default
 */
suspend fun launchScheduleUpdateCoroutine(messageId: Long) {
    while (true) {
        log(messageId, "coroutine delay has passed")
        getScheduleData(messageId).let {
            if (it != storedData[messageId]) {
                // TODO: make bot pin this message
                storedData[messageId] = it
                storedData[messageId]!!.displayInChat(messageId)
            }
        }
        // 1000L = 1 second
        delay(1000L * (updateTime[messageId]!!.first * 60 + updateTime[messageId]!!.second))
    }
}
