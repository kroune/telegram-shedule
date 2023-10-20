import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.entities.ChatId
import kotlinx.coroutines.*
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.forEachIndexed
import org.jetbrains.kotlinx.dataframe.api.getColumn
import org.jetbrains.kotlinx.dataframe.io.readCSV
import java.net.URL

var myCoroutine = CoroutineScope(Dispatchers.IO);
var storedData = mutableListOf<Pair<String, MutableList<Triple<String, String, String>>>>()
val bot = bot {
    token = ""
    dispatch {
        command("run") {
            bot.sendMessage(ChatId.fromId(message.chat.id), "starting bot")
            start(ChatId.fromId(message.chat.id))
            bot.sendMessage(ChatId.fromId(message.chat.id), "starting bot1")
        }
        command("update") {

        }
    }
}

fun start(fromId: ChatId.Id) {
    myCoroutine.launch {
        launchThr(fromId)
    }
}

//https://docs.google.com/spreadsheets/d/1L9UjNOZx4p4VER11SCyU97M07QnfWsZWwldAAOR0gtM/edit#gid=1229684819
fun main() {
    //getData()
    bot.startPolling()
}

suspend fun launchThr(fromId: ChatId.Id) {
    while (true) {
        getData().let {
            if (it != storedData) {
                storedData = it
                println("changed")
                storedData.display()
                storedData.displayInChat(fromId)
            }
        }
        println("${System.currentTimeMillis()}")
        delay(10000L)
    }
}

private fun MutableList<Pair<String, MutableList<Triple<String, String, String>>>>.display() {
    this.forEach {
        println(" ${it.first} ")
        it.second.forEach { info ->
            println("${info.first} в ${info.third} (${info.second}) ")
        }
        println()
    }
}

private fun MutableList<Pair<String, MutableList<Triple<String, String, String>>>>.displayInChat(chatId: ChatId) {
    this.forEach {
        var str = " ${it.first} "
        it.second.forEach { info ->
            str += "\n"
            str += "${info.first} {в ${info.third}} (${info.second})"
        }
        bot.sendMessage(chatId, str)
    }
}

fun getData(): MutableList<Pair<String, MutableList<Triple<String, String, String>>>> {
    val formattedData = mutableListOf<Pair<String, MutableList<Triple<String, String, String>>>>()
    lateinit var currentDay: Pair<String, MutableList<Triple<String, String, String>>>;
    val data =
        DataFrame.readCSV(URL("https://docs.google.com/spreadsheets/d/1L9UjNOZx4p4VER11SCyU97M07QnfWsZWwldAAOR0gtM/gviz/tq?tqx=out:csv&sheet=idk"))
    data.getColumn(0).forEachIndexed { index, it ->
        if (!it.empty()) {
            if (index != 0) formattedData.add(currentDay)
            currentDay = Pair(it!!.toString(), mutableListOf())
        }
        val ind = data.getColumnIndex("10Д")
        if (!data.getColumn(ind)[index].empty()) {
            currentDay.second.add(
                Triple(
                    data.getColumn(ind)[index].clarified(),
                    data.getColumn(ind + 1)[index].clarified(),
                    data.getColumn(ind + 1)[index + 1].clarified()
                )
            )
            data.getColumn(ind)[index].output()
            data.getColumn(ind + 1)[index].output()
            data.getColumn(ind + 1)[index + 1].output()
        }
    }
    formattedData.add(currentDay)
    return formattedData
}

private fun Any?.output() {
    if (!this.empty()) {
        //println(this)
    }
}

private fun Any?.clarified(): String {
    return if (!this.empty()) {
        this.toString()
    } else {
        ""
    }
}

private fun Any?.empty(): Boolean {
    return (this.toString() == "null")
}