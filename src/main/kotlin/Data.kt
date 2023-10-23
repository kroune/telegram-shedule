import com.elbekd.bot.types.Message
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.forEachIndexed
import org.jetbrains.kotlinx.dataframe.api.getColumn
import org.jetbrains.kotlinx.dataframe.io.readCSV
import java.net.URL

/**
 * loads data from provided url and converts it to mutableList
 */
fun getScheduleData(chatId: Message): MutableList<Pair<String, MutableList<Triple<String, String, String>>>> {
    @Suppress("SpellCheckingInspection") val data =
        DataFrame.readCSV(URL("${chosenLink[chatId.chat.id]}/gviz/tq?tqx=out:csv"))

    val formattedData = mutableListOf<Pair<String, MutableList<Triple<String, String, String>>>>()
    // schedule for a day
    lateinit var currentDay: Pair<String, MutableList<Triple<String, String, String>>>

    data.getColumn(0).forEachIndexed { index, element ->
        if (!element.empty()) {
            if (index != 0) formattedData.add(currentDay)
            // clears currentDay value
            currentDay = Pair(element!!.toString(), mutableListOf())
        }
        val classColumnIndex = data.getColumnIndex(chosenClass[chatId.chat.id]!!)
        if (!data.getColumn(classColumnIndex)[index].empty()) {
            /*
            * it is located like
            *
            * subject teacher
            *        classroom
            */
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

/**
 * it displays all data in the chat like this:
 *
 *   Monday
 *  PE {в 13} (Ivan Ivanov)
 *  Math {в 1} (Ivan Nikolay)
 *  etc...
 */
suspend fun MutableList<Pair<String, MutableList<Triple<String, String, String>>>>.displayInChat(chatId: Message) {
    this.forEach {
        var str = " ${it.first} "
        it.second.forEach { (lesson, teacher, classroom) ->
            str += "\n"
            str += "$lesson {в $classroom} ($teacher)"
        }
        bot.sendMessage(chatId, str)
    }
}

/**
 * it is used to prevent "null" s, which appear if a line is missing from going to the chat
 */
fun Any?.removeNull(): String {
    return if (!this.empty()) this.toString()
    else ""
}

/**
 * it is used to check if data, we read is empty
 */
fun Any?.empty(): Boolean {
    return this.toString() == "null"
}