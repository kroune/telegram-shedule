import data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import telegram.*

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
 * @param className we are working for
 */
suspend fun initializeChatValues(chatId: Long, className: String) {
    initializedBot.add(chatId)
    chosenClass[chatId] = className
    updateTime[chatId] = Pair(0, 30)
    pinErrorShown[chatId] = false
    log(chatId, "initializing variables", LogLevel.Info)
    log(chatId, "class name - $className, chosen link - $DEFAULT_LINK", LogLevel.Debug)
    storeConfigs(chatId, className, Pair(0, 30), storedSchedule[chatId], pinErrorShown[chatId]!!)
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
                log(chatId, "coroutine delay has passed", LogLevel.Info)
                getScheduleData(chatId).let {
                    if (!storedSchedule[chatId].matchesWith(it)) {
                        it.displayInChat(chatId, true)
                        sendMessage(
                            chatId,
                            "Похоже расписание обновилось, если это не так, свяжитесь с создателем бота (@LichnyiSvetM)"
                        )
                    }
                }
                processSchedulePinning(chatId)
                // 1000L = 1 second
                log(
                    chatId,
                    "coroutine delay - ${(updateTime[chatId]!!.first * 3600 + updateTime[chatId]!!.second * 60)}",
                    LogLevel.Debug
                )
                delay(1000L * (updateTime[chatId]!!.first * 3600 + updateTime[chatId]!!.second * 60))
            }
        } catch (e: CancellationException) {
            log(chatId, "Cancellation exception caught, this is expected \n$e", LogLevel.Debug)
        } catch (e: Exception) {
            log(chatId, e.stackTraceToString(), LogLevel.Error)
        }
    }
}
