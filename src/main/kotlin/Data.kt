import com.elbekd.bot.model.toChatId
import com.elbekd.bot.types.Message
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.forEachIndexed
import org.jetbrains.kotlinx.dataframe.api.getColumn
import org.jetbrains.kotlinx.dataframe.io.readCSV
import java.net.URL

fun getData(): MutableList<Pair<String, MutableList<Triple<String, String, String>>>> {
    @Suppress("SpellCheckingInspection")
    val data = DataFrame.readCSV(URL("$chosenLink/gviz/tq?tqx=out:csv"))

    val formattedData = mutableListOf<Pair<String, MutableList<Triple<String, String, String>>>>()
    lateinit var currentDay: Pair<String, MutableList<Triple<String, String, String>>>

    data.getColumn(0).forEachIndexed { index, it ->
        if (!it.empty()) {
            if (index != 0) formattedData.add(currentDay)
            currentDay = Pair(it!!.toString(), mutableListOf())
        }
        val classColumnIndex = data.getColumnIndex(chosenClass)
        if (!data.getColumn(classColumnIndex)[index].empty()) {
            currentDay.second.add(
                Triple(
                    data.getColumn(classColumnIndex)[index].removeNull(),
                    data.getColumn(classColumnIndex + 1)[index].removeNull(),
                    data.getColumn(classColumnIndex + 1)[index + 1].removeNull()
                )
            )
        }
    }
    formattedData.add(currentDay)
    return formattedData
}

suspend fun MutableList<Pair<String, MutableList<Triple<String, String, String>>>>.displayInChat(chatId: Message) {
    this.forEach {
        var str = " ${it.first} "
        it.second.forEach { (first, second, third) ->
            str += "\n"
            str += "$first {в $third} ($second)"
        }
        bot.sendMessage(chatId, str)
    }
}

private fun Any?.removeNull(): String {
    return if (!this.empty()) {
        this.toString()
    } else {
        ""
    }
}

private fun Any?.empty(): Boolean {
    return (this.toString() == "null")
}