package telegram

import checkClass
import com.elbekd.bot.feature.chain.chain
import com.elbekd.bot.feature.chain.terminateChain
import data.Config
import data.Config.configs
import data.Config.updateJob
import data.debug
import data.info
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
    buildRePinChain()
    buildNotificationChain()
    buildHelpChain()
}

/**
 * it creates /run chain (command), which should be executed to start the bot
 */
fun buildRunChain() {
    bot.chain("/start") {
        val chatId = it.chat.id
        info(chatId, "started /start chain")
        if (Config.hasStartedBot(chatId)) {
            info(chatId, "/start chain stopped due to bot, not being initialized")
            sendMessage(chatId, "Вы уже запускали бота, используйте команды настроек")
            bot.terminateChain(chatId)
            return@chain
        } else {
            debug(chatId, "/start chain started")
            sendMessage(chatId, "Это чат бот, который будет отправлять расписание в ваш чат (создан @LichnyiSvetM)")
            sendMessage(chatId, "Назовите ваш класс (например 10Д)")
        }

    }.then {
        val chatId = it.chat.id
        it.text!!.checkClass(chatId).let { checkedString ->
            if (checkedString != null) {
                sendMessage(chatId, "Полученный класс - \"${checkedString}\"")
                initializeChatValues(chatId, checkedString)
            } else sendMessage(chatId, "Класс введен не верно")
        }
    }.build()
}

/**
 * it updates schedule manually
 */
fun buildUpdateChain() {
    bot.onCommand("/update") {
        val chatId = it.first.chat.id
        info(chatId, "starting /update chain")
        if (!Config.hasStartedBot(chatId)) {
            info(chatId, "/update chain stopped due to bot, not being initialized")
            sendMessage(chatId, "Вам нужно выполнить команду /start чтобы инициализировать бота")
            return@onCommand
        }
        if (updateJob[chatId] != null) {
            updateJob[chatId]!!.cancel()
            updateJob[chatId] = null
        }
        debug(chatId, "/update chain started")
        scheduleUpdateCoroutine(chatId)
        sendMessage(chatId, "Успешно обновлено (будут обновлены закрепленные сообщения)")
    }
}

/**
 * it used to display useful information
 */
fun buildHelpChain() {
    bot.onCommand("/help") {
        @Suppress("SpellCheckingInspection") sendMessage(
            it.first.chat.id, """
            /help - выводит полезную информацию
            /notify - позволяет выключить/включить уведомления по поводу изменения расписания
            /output - позволяет принудительно вывести расписание (обычно не требуется)
            /update - позволяет обновить расписание не дожидаясь планового обновления
            /changeclass - позволяет изменить класс, для которого выводится расписание
            /start - запускает бота
            @LichnyiSvetM - создатель бота
            https://github.com/svetlichnyiMaxim/telegram-shedule - исходный код бота
            https://t.me/+RbguFVv9dP85OTEy - тг канал для тех, кому хочется узнать больше про бота
        """.trimIndent()
        )
    }
}

/**
 * it used for changing pinning settings
 */
fun buildRePinChain() {
    bot.onCommand("/repin") {
        val chatId = it.first.chat.id
        if (!Config.hasStartedBot(chatId)) {
            sendAsyncMessage(chatId, "Вам нужно вначале выполнить команду /start чтобы инициализировать бота")
            return@onCommand
        }
        if (configs[chatId]!!.shouldRePin) {
            sendMessage(chatId, "Сегодняшнее расписание больше не будет закрепляться")
        } else {
            sendMessage(chatId, "Теперь сегодняшнее расписание теперь будет закрепляться")
        }
        configs[chatId]!!.shouldRePin = !configs[chatId]!!.shouldRePin
    }
}

/**
 * it used for changing notifications settings
 */
fun buildNotificationChain() {
    bot.onCommand("/notify") {
        val chatId = it.first.chat.id
        if (!Config.hasStartedBot(chatId)) {
            sendAsyncMessage(chatId, "Вам нужно вначале выполнить команду /start чтобы инициализировать бота")
            return@onCommand
        }
        if (configs[chatId]!!.notifyAboutChanges) {
            sendMessage(chatId, "Вы больше не будете получать уведомления при обновлении расписания")
        } else {
            sendMessage(chatId, "Теперь вы будете получать уведомления при обновлении расписания")
        }
        configs[chatId]!!.notifyAboutChanges = !configs[chatId]!!.notifyAboutChanges
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
        val chatId = it.first.chat.id
        info(chatId, "starting /output chain")
        if (!Config.hasStartedBot(chatId)) {
            sendAsyncMessage(chatId, "Вам нужно вначале выполнить команду /start чтобы инициализировать бота")
            return@onCommand
        }
        if (updateJob[chatId] != null) {
            updateJob[chatId]!!.cancel()
            updateJob[chatId] = null
        }
        debug(chatId, "/output chain started")
        getScheduleData(chatId)?.let { schedule ->
            if (schedule.empty()) return@let
            schedule.displayInChat(chatId, true)
            if (configs[chatId]!!.shouldRePin) processSchedulePinning(chatId)
        }
    }
}

/**
 * it stops bot (should only be used by author)
 */
fun buildChangeClassChain() {
    // telegram only accepts lower-cased names
    @Suppress("SpellCheckingInspection") bot.chain("/changeclass") {
        val chatId = it.chat.id
        if (!Config.hasStartedBot(chatId)) {
            sendAsyncMessage(chatId, "Вам нужно вначале выполнить команду /start чтобы инициализировать бота")
            bot.terminateChain(chatId)
            return@chain
        }
        debug(chatId, "класс chain started")
        sendMessage(chatId, "Назовите ваш класс (например 10Д)")
    }.then {
        val chatId = it.chat.id
        it.text!!.checkClass(chatId).let { checkedString ->
            if (checkedString != null) {
                sendMessage(chatId, "Класс успешно обновлён")
                configs[chatId]!!.className = checkedString
                if (updateJob[chatId] != null) {
                    updateJob[chatId]!!.cancel()
                    updateJob[chatId] = null
                }
                scheduleUpdateCoroutine(chatId)
                debug(chatId, "Success update")
                sendMessage(chatId, "Успешно обновлено (будут обновлены закрепленные сообщения при наличие)")
            } else {
                info(chatId, "Wrong format")
                sendMessage(chatId, "Не правильный формат ввода класса")
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
        } else {
            bot.terminateChain(it.chat.id)
        }

    }.then {
        if (it.text == "/confirm_$confirmation") {
            println("bot stopped")
            bot.stop()
        }
    }.build()
}
