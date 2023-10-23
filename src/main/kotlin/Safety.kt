/**
 * checks if a link is safe
 * note that every valid Google link contains 83 chars
 */
fun String.checkLink(): String? {
    return when (this.length) {
        83 -> if (this.startsWith("https://docs.google.com/spreadsheets/d/")) this else null

        44 -> "https://docs.google.com/spreadsheets/d/${this}"

        else -> null
    }
}

/**
 * checks if class name is valid
 */
fun String.checkClass(): String? {
    return if (this.length == 3 && this.last().isLetter() && this.drop(1).all { it.isDigit() })
        this
    else
        null
}