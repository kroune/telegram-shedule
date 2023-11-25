import data.DEFAULT_LINK
import data.listOfClasses
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.forEachIndexed
import org.jetbrains.kotlinx.dataframe.api.getColumn
import org.jetbrains.kotlinx.dataframe.io.readCSV
import org.jetbrains.kotlinx.dataframe.size
import java.net.URI

/**
 * checks if class name is valid
 */
fun String.checkClass(): String? {
    return if (this.length in 2..3 && this.last().isLetter() && listOfClasses.contains(
            this.dropLast(1) + this.last().uppercase()
        ) && verifyClassNameAvailability(this.dropLast(1) + this.last().uppercase())
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
    @Suppress("SpellCheckingInspection") val data =
        DataFrame.readCSV(URI.create("$DEFAULT_LINK/gviz/tq?tqx=out:csv").toURL())
    try {
        data.getColumnOrNull(1)?.forEachIndexed { index, element ->
            data.getColumn(0)[index]
            if (index == data.size().nrow - 1 || element.removeNull()
                    .any { !it.isDigit() } || element.empty()
            ) return@forEachIndexed

            val classColumnIndex = data.getColumnIndex(className)
            val subject = data.getColumn(classColumnIndex)[index].removeNull()

            if (subject.isNotEmpty() && subject.isNotBlank()) {
                data.getColumn(classColumnIndex + 1)[index]
                data.getColumn(classColumnIndex + 1)[index + 1]
            }
        }
    } catch (e: IllegalArgumentException) {
        println("link verification failed due to \n$e")
        return false
    } catch (e: IndexOutOfBoundsException) {
        println("link verification failed due to \n$e")
        return false
    }
    return true
}
