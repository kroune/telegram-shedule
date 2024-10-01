package io.github.kroune.translation

import kotlinx.datetime.DayOfWeek

/**
 * Repository for translations used in bot
 */
@Suppress("UndocumentedPublicProperty")
sealed interface TranslationRepositoryI {
    val startResponse: String
    val classResponse: String
    val classNotFoundResponse: String
    val availableClassesList: String
    val classSelectedResponse: String
    val outputModeChangeResponse: String
    val outputModeChangeSuccess: String
    val outputModeList: String
    val outputModeNotFound: String
    val botInfo: String
    val editingOldMessagesFailedCommaResending: String

    /**
     * Returns name of the day in local language
     */
    fun DayOfWeek.nameInLocalLang(): String
}
