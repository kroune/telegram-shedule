import data.info
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.forEachIndexed
import org.jetbrains.kotlinx.dataframe.api.getColumn
import org.jetbrains.kotlinx.dataframe.io.readCSV
import org.jetbrains.kotlinx.dataframe.size
import java.net.URI


/**
 * default schedule link
 */
const val DEFAULT_LINK: String = "https://docs.google.com/spreadsheets/d/1L9UjNOZx4p4VER11SCyU97M07QnfWsZWwldAAOR0gtM"

/**
 * it is used to make sure user did everything correctly
 */
val listOfClasses: Collection<String> = listOf(
    "2А",
    "2Б",
    "2В",
    "2Г",
    "3А",
    "3Б",
    "3В",
    "3Г",
    "4А",
    "4Б",
    "4В",
    "4Г",
    "5А",
    "5Б",
    "5В",
    "5Г",
    "5И",
    "5П",
    "6А",
    "6Б",
    "6В",
    "6Г",
    "6П",
    "7В",
    "7О",
    "7П",
    "7А",
    "7Б",
    "7Г",
    "8А",
    "8Б",
    "8В",
    "8Г",
    "8Д",
    "8М",
    "8Е",
    "8У",
    "8Ф",
    "8Я",
    "9А",
    "9В",
    "9Г",
    "9Д",
    "9М",
    "9Е",
    "9П",
    "9У",
    "9Ф",
    "9Х",
    "9Я",
    "10Б",
    "10В",
    "10Г",
    "10Д",
    "10М",
    "10Е",
    "10И",
    "10С",
    "10У",
    "10Ф",
    "10Я",
    "11А",
    "11В",
    "11Г",
    "11Д",
    "11М",
    "11Е",
    "11И",
    "11У",
    "11Ф",
    "11Я"
)

/**
 * checks if class name is valid
 */
fun String.checkClass(chatId: Long): String? {
    val properFormatCheck = this.length in 2..3 && this.last().isLetter()
    if (!properFormatCheck) info(chatId, "format check failed $this")
    val isItInListOfClasses = listOfClasses.contains(this.dropLast(1) + this.last().uppercase())
    info(chatId, "format check failed status - ${if (isItInListOfClasses) "success" else "FAILED"} $this")
    return if (properFormatCheck && isItInListOfClasses && verifyClassNameAvailability(
            this.dropLast(1) + this.last().uppercase()
        )
    ) this.dropLast(1) + this.last().uppercase()
    else null
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

/**
 * this is used for quick verification of class name, doing it only on update leads to more bugs and code complexity
 * @param className name of the class we want to verify
 */
fun verifyClassNameAvailability(className: String): Boolean {
    try {
        @Suppress("SpellCheckingInspection") val data =
            DataFrame.readCSV(URI.create("$DEFAULT_LINK/gviz/tq?tqx=out:csv").toURL())

        data.getColumnOrNull(1)?.forEachIndexed { index, element ->
            data.getColumn(0)[index]
            if (index == data.size().nrow - 1 || element.removeNull()
                    .any { !it.isDigit() } || element.empty()
            ) return@forEachIndexed

            val upperCase = data.getColumnIndex(className.uppercase()) != -1
            val classColumnIndex = data.getColumnIndex(if (upperCase) className else className.lowercase())
            val subject = data.getColumn(classColumnIndex)[index].removeNull()

            if (subject.isNotEmpty() && subject.isNotBlank()) {
                data.getColumn(classColumnIndex + 1)[index]
                data.getColumn(classColumnIndex + 1)[index + 1]
            }
        }
    } catch (e: IllegalArgumentException) {
        println("link verification failed due to \n${e.stackTraceToString()}")
        return false
    } catch (e: IndexOutOfBoundsException) {
        println("link verification failed due to \n${e.stackTraceToString()}")
        return false
    }
    return true
}
