package io.github.kroune.translation

import kotlinx.datetime.DayOfWeek

/**
 * Russian translations
 */
@Suppress("MaxLineLength")
class RussianTranslationRepositoryImpl: TranslationRepositoryI {
    override val startResponse = "Привет! Этот бот выводит и сообщяет об обновлениях расписания занятий выбранного класса. Для начала работы введите /class"
    override val classResponse = "Для того чтобы изменить текущий класс, введите его (например 11д/9м/т.п.)"
    override val classNotFoundResponse = "Класс не найден. Пожалуйста, попробуйте еще раз."
    override val availableClassesList = "Список доступных классов:"
    override val classSelectedResponse = "Вы успешно выбрали класс"
    override val outputModeChangeResponse = "Для изменения режима вывода введите названия режима"
    override val outputModeChangeSuccess = "Режим вывода успешно изменен"
    override val outputModeList = "Список доступных классов:"
    override val outputModeNotFound = "Формат вывода не найден"
    override val botInfo = "Бот создан @LichnyiSvetM, при возникновение проблем с ботом пишите в лс"
    override val editingOldMessagesFailedCommaResending = "Редактирование старых сообщений не удалось, отправляю новые"

    override fun DayOfWeek.nameInLocalLang(): String {
        return when (this) {
            DayOfWeek.MONDAY -> "Понедельник"
            DayOfWeek.TUESDAY -> "Вторник"
            DayOfWeek.WEDNESDAY -> "Среда"
            DayOfWeek.THURSDAY -> "Четверг"
            DayOfWeek.FRIDAY -> "Пятница"
            DayOfWeek.SATURDAY -> "Суббота"
            DayOfWeek.SUNDAY -> "Воскресенье"
        }
    }
}
