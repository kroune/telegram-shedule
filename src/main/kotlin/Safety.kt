fun String.checkLink(): String? {
    when (this.length) {
        83 -> {
            return if (this.startsWith("https://docs.google.com/spreadsheets/d/")) this else null
        }

        44 -> {
            return if (this.any { !it.isLetterOrDigit() }) {
                null
            } else {
                "https://docs.google.com/spreadsheets/d/${this}"
            }
        }

        else -> {
            return null
        }
    }
}