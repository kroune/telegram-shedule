import data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import telegram.*
import java.time.LocalDate

/**
 * here we initialize our commands and that bot
 */
suspend fun main() {
    initializeChains()
    loadData()
    bot.start()
}

/**
 * initialize default values for a new chat
 * @param chatId id of telegram chat
 */
suspend fun initializeChatValues(chatId: Long, className: String) {
    initializedBot.add(chatId)
    previousDay[chatId] = LocalDate.now().dayOfWeek
    chosenClass[chatId] = className
    chosenLink[chatId] = defaultLink
    updateTime[chatId] = Pair(2, 0)
    log(chatId, "initializing variables")
    storeConfigs(chatId, className, defaultLink, Pair(0, 30), storedSchedule[chatId])
    launchScheduleUpdateCoroutine(chatId)
}

/**
 * this checks if schedule has changed every 30 minutes by default
 * @param chatId id of telegram chat
 */
suspend fun launchScheduleUpdateCoroutine(chatId: Long) {
    updateJob[chatId] = myScheduleCoroutine.launch {
        try {
            while (true) {
                log(chatId, "coroutine delay has passed")
                getScheduleData(chatId).let {
                    if (!storedSchedule[chatId].matchesWith(it)) {
                        it.displayInChat(chatId, true)
                        sendMessage(
                            chatId,
                            "Похоже расписание обновилось, если это не так, свяжитесь с создателем бота (@LichnyiSvetM)"
                        )
                    }
                }
                processPin(chatId)
                // 1000L = 1 second
                delay(1000L * (updateTime[chatId]!!.first * 3600 + updateTime[chatId]!!.second * 60))
            }
        } catch (e: CancellationException) {
            log(chatId, "Cancellation exception caught, this is expected")
        } catch (e: Exception) {
            println(e.cause)
            sendErrorMessage(chatId)
            log(chatId, e.stackTraceToString())
        }
    }
}
