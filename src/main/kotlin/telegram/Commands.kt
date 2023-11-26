package telegram

import checkClass
import com.elbekd.bot.feature.chain.chain
import com.elbekd.bot.feature.chain.terminateChain
import data.*
import empty
import initializeChatValues
import scheduleUpdateCoroutine

/**
 * it is used to initialize all chains of commands
 */
fun initializeChains() {
    buildRunChain()
    buildOutputChain()
    buildKillChain()
    buildUpdateChain()
    buildChangeClassChain()
    buildPingChain()
}

/**
 * it creates /run chain (command), which should be executed to start the bot
 */
fun buildRunChain() {
    bot.chain("/start") {
        log(it.chat.id, "started /start chain", LogLevel.Info)
        if (initializedBot.contains(it.chat.id)) {
            log(it.chat.id, "/start chain stopped due to bot, not being initialized", LogLevel.Info)
            sendMessage(it.chat.id, "Вы уже запускали бота, используйте команды настроек")
            bot.terminateChain(it.chat.id)
        } else {
            log(it.chat.id, "/start chain started", LogLevel.Debug)
            sendMessage(it.chat.id, "Это чат бот, который будет отправлять расписание в ваш чат (создан @LichnyiSvetM)")
            sendMessage(it.chat.id, "Назовите ваш класс (например 10Д)")
        }

    }.then {
        (it.text ?: return@then).checkClass().let { checkedString ->
            if (checkedString != null) {
                sendMessage(it.chat.id, "Полученный класс - \"${checkedString}\"")
                initializeChatValues(it.chat.id, checkedString)
            } else sendMessage(it.chat.id, "Класс введен не верно")
        }

    }.build()
}

/**
 * it updates schedule manually
 */
fun buildUpdateChain() {
    bot.onCommand("/update") {
        log(it.first.chat.id, "starting /update chain", LogLevel.Info)
        if (!initializedBot.contains(it.first.chat.id)) {
            log(it.first.chat.id, "/update chain stopped due to bot, not being initialized", LogLevel.Info)
            sendMessage(it.first.chat.id, "Вам нужно выполнить команду /start чтобы инициализировать бота")
            return@onCommand
        }
        if (updateJob[it.first.chat.id] != null) {
            updateJob[it.first.chat.id]!!.cancel()
            updateJob[it.first.chat.id] = null
        }
        log(it.first.chat.id, "/update chain started", LogLevel.Debug)
        scheduleUpdateCoroutine(it.first.chat.id)
        sendMessage(it.first.chat.id, "Успешно обновлено (будут обновлены закрепленные сообщения)")
    }
}

/**
 * it is used for checking if bot is down
 */
fun buildPingChain() {
    bot.onCommand("/ping") {
        sendMessage(it.first.chat.id, "pong")
    }
}

/**
 * it updates schedule manually
 */
fun buildOutputChain() {
    bot.onCommand("/output") {
        log(it.first.chat.id, "starting /output chain", LogLevel.Info)
        if (!initializedBot.contains(it.first.chat.id)) {
            log(it.first.chat.id, "/output chain stopped due to bot, not being initialized", LogLevel.Info)
            sendMessage(it.first.chat.id, "Вам нужно выполнить команду /start чтобы инициализировать бота")
            return@onCommand
        }
        if (updateJob[it.first.chat.id] != null) {
            updateJob[it.first.chat.id]!!.cancel()
            updateJob[it.first.chat.id] = null
        }
        log(it.first.chat.id, "/output chain started", LogLevel.Debug)
        getScheduleData(it.first.chat.id).let { schedule ->
            if (schedule.empty()) return@let
            schedule.displayInChat(it.first.chat.id, true)
            processSchedulePinning(it.first.chat.id)
        }
    }
}

/**
 * it stops bot (should only be used by author)
 */
fun buildChangeClassChain() {
    // telegram only accepts lower-cased names
    @Suppress("SpellCheckingInspection")
    bot.chain("/changeclass") {
        if (initializationCheckStatus(it.chat.id)) {
            bot.terminateChain(it.chat.id)
        }
        log(it.chat.id, "класс chain started", LogLevel.Debug)
        sendMessage(it.chat.id, "Назовите ваш класс (например 10Д)")
    }.then {
        (it.text ?: return@then).checkClass().let { checkedString ->
            if (checkedString != null) {
                sendMessage(it.chat.id, "Класс успешно обновлён")
                chosenClass[it.chat.id] = checkedString
                if (updateJob[it.chat.id] != null) {
                    updateJob[it.chat.id]!!.cancel()
                    updateJob[it.chat.id] = null
                }
                scheduleUpdateCoroutine(it.chat.id)
                log(it.chat.id, "Success update", LogLevel.Debug)
                sendMessage(it.chat.id, "Успешно обновлено (будут обновлены закрепленные сообщения при наличие)")
            } else {
                log(it.chat.id, "Wrong format", LogLevel.Info)
                sendMessage(it.chat.id, "Не правильный формат ввода класса")
            }
        }
    }.build()
}
/**
 * it stops bot (should only be used by author)
 */
fun buildKillChain() {
    var confirmation = ""
    bot.chain("/kill") {
        if ((it.from ?: return@chain).username == "LichnyiSvetM") {
            confirmation = System.currentTimeMillis().toString()
            sendMessage(it.chat.id, "do /confirm_$confirmation to force stop bot")
        } else bot.terminateChain(it.chat.id)

    }.then {
        if (it.text == "/confirm_$confirmation") {
            println("bot stopped")
            bot.stop()
        }
    }.build()
}

/**
 * it checks if bot was initialized and stops if it wasn't
 * @param chatId ID of telegram chat
 */
fun initializationCheckStatus(chatId: Long): Boolean {
    if (!initializedBot.contains(chatId)) {
        sendAsyncMessage(chatId, "Вам нужно выполнить команду /start чтобы инициализировать бота")
        bot.terminateChain(chatId)
        return true
    }
    return false
}
