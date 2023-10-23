import com.elbekd.bot.Bot
import com.elbekd.bot.model.toChatId
import com.elbekd.bot.types.Message
import kotlinx.coroutines.*

/**
 * token is generated per bot, should be deleted, when uploading somewhre
 */
@Suppress("SpellCheckingInspection")
const val token: String = "6722149681:AAFzavXlMrrYfimFunPC_-4fwVMnGWxc6pE"

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
    buildRunChain()
    buildUpdateChain()
    buildConfigureChain()
    bot.start()
}

/**
 * initialize default values for a new chat
 */
fun initializeChatValues(message: Message, className: String) {
    chosenClass[message.chat.id] = className
    chosenLink[message.chat.id] = "https://docs.google.com/spreadsheets/d/1L9UjNOZx4p4VER11SCyU97M07QnfWsZWwldAAOR0gtM"
    updateTime[message.chat.id] = Pair(2, 0)
    updateJob[message.chat.id] = myCoroutine.launch {
        launchScheduleUpdateCoroutine(message)
    }
}

/**
 * this way it takes less space
 * @param text is a string we want to output
 */
suspend fun Bot.sendMessage(message: Message, text: String) {
    this.sendMessage(message.chat.id.toString().toChatId(), text)
}

/**
 * this checks if schedule has changed every 2 hours by default
 */
suspend fun launchScheduleUpdateCoroutine(message: Message) {
    while (true) {
        getScheduleData(message).let {
            if (it != storedData[message.chat.id]) {
                // TODO: make bot pin this message
                storedData[message.chat.id] = it
                storedData[message.chat.id]!!.displayInChat(message)
            }
        }
        // 1000L = 1 second
        delay(1000L * (updateTime[message.chat.id]!!.first * 60 + updateTime[message.chat.id]!!.second))
    }
}
