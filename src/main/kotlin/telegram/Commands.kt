package telegram

import checkClass
import com.elbekd.bot.feature.chain.chain
import com.elbekd.bot.feature.chain.terminateChain
import data.*
import displayInChat
import empty
import getScheduleData
import initializeChatValues
import processSchedulePinning
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
    buildNotificationChain()
    buildHelpChain()
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
        it.text!!.checkClass().let { checkedString ->
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
        val id = it.first.chat.id
        log(id, "starting /update chain", LogLevel.Info)
        if (!initializedBot.contains(id)) {
            log(id, "/update chain stopped due to bot, not being initialized", LogLevel.Info)
            sendMessage(id, "Вам нужно выполнить команду /start чтобы инициализировать бота")
            return@onCommand
        }
        if (updateJob[id] != null) {
            updateJob[id]!!.cancel()
            updateJob[id] = null
        }
        log(id, "/update chain started", LogLevel.Debug)
        scheduleUpdateCoroutine(id)
        sendMessage(id, "Успешно обновлено (будут обновлены закрепленные сообщения)")
    }
}

/**
 * it used to display useful information
 */
fun buildHelpChain() {
    bot.onCommand("/help") {
        @Suppress("SpellCheckingInspection")
        sendMessage(it.first.chat.id, """
            /help - выводит полезную информацию
            /notify - позволяет выключить/включить уведомления по поводу изменения расписания
            /output - позволяет принудительно вывести расписание (обычно не требуется)
            /update - позволяет обновить расписание не дожидаясь планового обновления
            /changeclass - позволяет изменить класс, для которого выводится расписание
            /start - запускает бота
        """.trimIndent())
    }
}

/**
 * it used for changing notifications settings
 */
fun buildNotificationChain() {
    bot.onCommand("/notify") {
        val id = it.first.chat.id
        if (initializationCheckStatus(id)) {
            return@onCommand
        }
        if (notifyAboutScheduleChanges[id]!!) {
            sendMessage(id, "Вы больше не будете получать уведомления при обновлении расписания")
        } else {
            sendMessage(id, "Теперь вы будете получать уведомления при обновлении расписания")
        }
        notifyAboutScheduleChanges[id] = !notifyAboutScheduleChanges[id]!!
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
        val id = it.first.chat.id
        log(id, "starting /output chain", LogLevel.Info)
        if (initializationCheckStatus(id)) {
            return@onCommand
        }
        if (updateJob[id] != null) {
            updateJob[id]!!.cancel()
            updateJob[id] = null
        }
        log(id, "/output chain started", LogLevel.Debug)
        getScheduleData(id)?.let { schedule ->
            if (schedule.empty()) return@let
            schedule.displayInChat(id, true)
            processSchedulePinning(id)
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
        it.text!!.checkClass().let { checkedString ->
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
        if (it.from!!.username == "LichnyiSvetM") {
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
        sendAsyncMessage(chatId, "Вам нужно вначале выполнить команду /start чтобы инициализировать бота")
        bot.terminateChain(chatId)
        return true
    }
    return false
}
