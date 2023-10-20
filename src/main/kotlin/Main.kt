import com.elbekd.bot.Bot
import com.elbekd.bot.model.toChatId
import com.elbekd.bot.types.Message
import kotlinx.coroutines.*

@Suppress("SpellCheckingInspection")
const val token: String = ""

var chosenClass: String = ""
var chosenLink: String = "https://docs.google.com/spreadsheets/d/1L9UjNOZx4p4VER11SCyU97M07QnfWsZWwldAAOR0gtM"
var myCoroutine: CoroutineScope = CoroutineScope(Dispatchers.IO)
var updateTime: Pair<Int, Int> = Pair(2, 0)

var updateJob: Job? = null
var storedData: MutableList<Pair<String, MutableList<Triple<String, String, String>>>> = mutableListOf()
val bot: Bot = Bot.createPolling(token)

fun main() {
    buildRunChain()
    buildConfigureChain()
    bot.start()
}

fun start(fromId: Message) {
    updateJob = myCoroutine.launch {
        launchThr(fromId)
    }
}

suspend fun Bot.sendMessage(chatId: Message, text: String) {
    this.sendMessage(chatId.chat.id.toString().toChatId(), text)
}

suspend fun launchThr(fromId: Message) {
    while (true) {
        getData().let {
            if (it != storedData) {
                storedData = it
                println("data has changed")
                storedData.displayInChat(fromId)
            }
        }
        println("Updated at ${System.currentTimeMillis()}")
        delay(1000L * (updateTime.first * 60 + updateTime.second))
    }
}
