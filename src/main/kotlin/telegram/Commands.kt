package telegram

import checkClass
import checkLink
import com.elbekd.bot.feature.chain.chain
import com.elbekd.bot.feature.chain.terminateChain
import com.elbekd.bot.model.toChatId
import data.*
import initializeChatValues
import launchScheduleUpdateCoroutine

/**
 * it is used to initialize all chains of commands
 */
fun initializeChains() {
    buildRunChain()
    buildOutputChain()
    buildKillChain()
    buildUpdateChain()
    buildConfigureChain()
}

/**
 * it creates /run chain (command), which should be executed to start the bot
 */
fun buildRunChain() {
    bot.chain("/start") {
        log(it.chat.id, "started /start chain", LogLevel.Info)
        if (initializedBot.contains(it.chat.id)) {
            log(it.chat.id, "/start chain stopped due to bot, not being initialized", LogLevel.Info)
            sendMessage(it.chat.id, "Вы уже запускали бота, используйте команду /configure если хотите настроить бота")
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
        launchScheduleUpdateCoroutine(it.first.chat.id)
        sendMessage(it.first.chat.id, "Успешно обновлено (будут обновлены закрепленные сообщения)")
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
        log(it.first.chat.id, "/output chain started", LogLevel.Debug)
        storedSchedule[it.first.chat.id]!!.displayInChat(it.first.chat.id, false)
    }
}

/**
 * it stops bot (should only be used by author)
 */
fun buildKillChain() {
    var confirmation = ""
    bot.chain("/kill") {
        if (it.from!!.username == "LichnyiSvetM") {
            confirmation = System.currentTimeMillis().toString()
            bot.sendMessage(it.chat.id.toChatId(), "do /confirm_$confirmation to force stop bot")
        } else bot.terminateChain(it.chat.id)

    }.then {
        if (it.text == "/confirm_$confirmation") {
            println("bot stopped")
            bot.stop()
        }
    }.build()
}

/**
 * it creates /configure chain (command), which should be executed to configure the bot
 */
fun buildConfigureChain() {
    var stringState = 0
    bot.chain("/configure") {
        log(it.chat.id, "starting /configure chain", LogLevel.Info)
        if (!initializedBot.contains(it.chat.id)) {
            log(it.chat.id, "/configure chain stopped due to bot, not being initialized", LogLevel.Info)
            sendMessage(it, "Вам нужно выполнить команду /start чтобы инициализировать бота")
            bot.terminateChain(it.chat.id)
            return@chain
        }
        sendMessage(
            it.chat.id, """
            Список настроек: {
              'класс'
              'ссылка' (ссылка на расписание)
            }
            выведите 'стоп' если хотите завершить настройку     
            """.trimMargin()
        )
    }.then {
        when (it.text?.lowercase()) {
            "stop", "стоп" -> bot.terminateChain(it.chat.id)

            "class", "класс" -> {
                log(it.chat.id, "класс chain started", LogLevel.Debug)
                sendMessage(it.chat.id, "Назовите ваш класс (например 10Д)")
                stringState = 0
            }

            "link", "ссылка" -> {
                log(it.chat.id, "ссылка chain started", LogLevel.Debug)
                sendMessage(it.chat.id, "Введите ссылку на расписание (например $defaultLink)")
                stringState = 1
            }

            else -> {
                log(it.chat.id, "wrong name", LogLevel.Debug)
                sendMessage(it.chat.id, "Бот не смог распознать вашу команду")
                bot.terminateChain(it.chat.id)
            }
        }
    }.then {
        when (stringState) {
            0 -> {
                it.text!!.checkClass().let { checkedString ->
                    if (checkedString != null) {
                        sendMessage(it.chat.id, "Класс успешно обновлён")
                        chosenClass[it.chat.id] = checkedString
                        if (updateJob[it.chat.id] != null) {
                            updateJob[it.chat.id]!!.cancel()
                            updateJob[it.chat.id] = null
                        }
                        launchScheduleUpdateCoroutine(it.chat.id)
                        log(it.chat.id, "Success update", LogLevel.Debug)
                        sendMessage(it.chat.id, "Успешно обновлено (будут обновлены закрепленные сообщения)")
                    } else {
                        log(it.chat.id, "Wrong format", LogLevel.Info)
                        sendMessage(it.chat.id, "Не правильный формат ввода класса")
                    }
                }
            }

            1 -> {
                it.text!!.checkLink().let { result ->
                    if (result == null) sendMessage(it.chat.id, "Не правильная ссылка")
                    else {
                        sendMessage(it.chat.id, "Ссылка обновлена")
                        chosenLink[it.chat.id] = result
                        if (updateJob[it.chat.id] != null) {
                            updateJob[it.chat.id]!!.cancel()
                            updateJob[it.chat.id] = null
                        }
                        launchScheduleUpdateCoroutine(it.chat.id)
                        log(it.chat.id, "Success link update", LogLevel.Debug)
                        sendMessage(it.chat.id, "Успешно обновлено (будут обновлены закрепленные сообщения)")
                    }
                }
            }
        }

        storeConfigs(
            it.chat.id,
            chosenClass[it.chat.id]!!,
            chosenLink[it.chat.id]!!,
            updateTime[it.chat.id]!!,
            storedSchedule[it.chat.id]!!
        )
    }.build()
}