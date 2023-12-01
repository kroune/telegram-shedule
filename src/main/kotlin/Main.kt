import data.*
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
 * when uploading somewhere, this token should be deleted;
 * this one is used for testing stuffs
 */
@Suppress("SpellCheckingInspection", "RedundantSuppression")
const val TEST_TOKEN: String = ""

/**
 * when uploading somewhere, this token should be deleted;
 * this one is used for production stuffs
 */
@Suppress("SpellCheckingInspection", "RedundantSuppression")
const val PRODUCTION_TOKEN: String = ""

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
    initializedBot.add(chatId)
    chosenClass[chatId] = className
    pinErrorShown[chatId] = false
    notifyAboutScheduleChanges[chatId] = true
    storedSchedule[chatId] = UserSchedule(mutableListOf())
    log(chatId, "initializing variables", LogLevel.Info)
    log(chatId, "class name - $className, chosen link - $DEFAULT_LINK", LogLevel.Debug)
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
                    if (!storedSchedule[chatId].matchesWith(it)) {
                        it.displayInChat(chatId, false)
                        if (notifyAboutScheduleChanges[chatId]!!) sendMessage(
                            chatId,
                            "Расписание обновилось, если это не так, свяжитесь с создателем бота (@LichnyiSvetM)"
                        )
                    }
                }
                processSchedulePinning(chatId)
            }
        }
        log(chatId, "schedule update took $timeTaken", LogLevel.Debug)
        true
    } catch (e: CancellationException) {
        log(chatId, "this is expected \n ${e.stackTraceToString()}", LogLevel.Debug)
        return true
    } catch (e: Exception) {
        log(chatId, "an exception occurred, while updating schedule \n ${e.stackTraceToString()}", LogLevel.Error)
        false
    }
}

/**
 * checks if previous schedule messages exist
 * @param chatId ID of telegram chat
 */
suspend fun scheduleExists(chatId: Long): Boolean {
    storedSchedule[chatId]!!.messages.forEach {
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
    updateJob[chatId] = CoroutineScope(Dispatchers.Default).launch {
        try {
            var failedAttempts = 0
            while (isActive && failedAttempts < 5) {
                log(chatId, "coroutine delay has passed", LogLevel.Debug)
                if (!updateSchedule(chatId)) {
                    delay(5000)
                    failedAttempts++
                    continue
                }

                failedAttempts = max(failedAttempts - 1, 0)
                // 1000L = 1 second
                (updateTime.toInt(DurationUnit.SECONDS)).let { delay ->
                    log(chatId, "coroutine delay - $delay", LogLevel.Debug)
                    delay(1000L * delay)
                }
            }
            sendMessage(
                chatId,
                "Произошло слишком много неожиданных ошибок, бот прекратил обновление расписания, пока " +
                        "разработчик не разберется с этой проблемой. Используйте /update для перезапуска"
            )
            this.cancel()
            updateJob[chatId] = null
        } catch (e: CancellationException) {
            log(chatId, "Cancellation exception caught, this is expected \n ${e.stackTraceToString()}", LogLevel.Debug)
        }
    }
}
