import data.*
import data.Config.configs
import data.Config.updateJob
import kotlinx.coroutines.*
import telegram.bot
import telegram.exists
import telegram.initializeChains
import telegram.sendMessage
import kotlin.math.max
import kotlin.system.measureTimeMillis
import kotlin.time.DurationUnit

/**
 * this is used for testing purposes
 */
const val IS_TEST: Boolean = false

/**
 * here we initialize our commands and that bot
 */
fun main() {
    initializeChains()
    loadData()
    bot.start()
}

/**
 * initialize default values for a new chat
 * @param chatId ID of telegram chat
 * @param className we are working for
 */
fun initializeChatValues(chatId: Long, className: String) {
    configs[chatId] = ConfigData(
        className = className,
        schedule = UserSchedule(mutableListOf()),
        pinErrorShown = false,
        notifyAboutChanges = true,
        shouldRePin = true,
        initializedBot = true
    )
    info(chatId, "initializing variables")
    debug(chatId, "class name - $className, chosen link - $DEFAULT_LINK")
    scheduleUpdateCoroutine(chatId)
    storeConfigs(chatId)
}

/**
 * this checks if schedule has changed
 * @param chatId ID of telegram chat
 * @return if update was successful
 */
suspend fun updateSchedule(chatId: Long): Boolean {
    return try {
        val timeTaken = measureTimeMillis {
            getScheduleData(chatId)?.let {
                if (it.empty()) return@let
                if (!scheduleExists(chatId)) {
                    sendMessage(chatId, "Не удалось найти предыдущие сообщения")
                    it.displayInChat(chatId, true)
                } else {
                    if (!configs[chatId]!!.schedule.matchesWith(it)) {
                        it.displayInChat(chatId, false)
                        if (configs[chatId]!!.notifyAboutChanges) sendMessage(
                            chatId,
                            "Расписание обновилось, если это не так, свяжитесь с создателем бота (@LichnyiSvetM)"
                        )
                    }
                }
                if (configs[chatId]!!.shouldRePin) processSchedulePinning(chatId)
            }
        }
        debug(chatId, "schedule update took $timeTaken")
        true
    } catch (e: CancellationException) {
        debug(chatId, "this is expected \n ${e.stackTraceToString()}")
        return true
    } catch (e: Exception) {
        error(chatId, "an exception occurred, while updating schedule \n ${e.stackTraceToString()}")
        false
    }
}

/**
 * checks if previous schedule messages exist
 * @param chatId ID of telegram chat
 */
suspend fun scheduleExists(chatId: Long): Boolean {
    configs[chatId]!!.schedule.messages.forEach {
        it.messageInfo.messageId.let { id ->
            if (id == -1L || !id.exists(chatId)) {
                return false
            }
        }
    }
    return true
}

/**
 * this every 30-minute calls schedule update
 * @param chatId ID of telegram chat
 */
fun scheduleUpdateCoroutine(chatId: Long) {
    @Volatile
    updateJob[chatId] = CoroutineScope(Dispatchers.Default).async {
        try {
            var failedAttempts = 0
            while (isActive && failedAttempts < 5) {
                debug(chatId, "coroutine delay has passed")
                if (!updateSchedule(chatId)) {
                    delay(5000)
                    failedAttempts++
                    continue
                }

                failedAttempts = max(failedAttempts - 1, 0)
                // 1000L = 1 second
                (updateTime.toInt(DurationUnit.SECONDS)).let { delay ->
                    debug(chatId, "coroutine delay - $delay")
                    delay(1000L * delay)
                }
            }
            sendMessage(
                chatId, """Произошло слишком много неожиданных ошибок. 
                    бот прекратил обновление расписания, до тех пор, пока разработчик не разберется с этой проблемой. 
                    Используйте /update для перезапуска""".trimMargin()
            )
            this.cancel()
            updateJob[chatId] = null
        } catch (e: CancellationException) {
            debug(chatId, "Cancellation exception caught, this is expected \n ${e.stackTraceToString()}")
        }
    }
}
