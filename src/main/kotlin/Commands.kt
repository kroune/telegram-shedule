import com.elbekd.bot.feature.chain.chain
import com.elbekd.bot.feature.chain.terminateChain
import kotlinx.coroutines.launch
import logger.log
import logger.storeConfigs

/**
 * it creates /run chain (command), which should be executed to start the bot
 */
fun buildRunChain() {
    bot.chain("/run") {
        log(it.chat.id, "starting /run chain")
        if (initializedBot.contains(it.chat.id)) {
            sendMessage(
                it.chat.id,
                "Вы уже запускали бота, используйте команду /configure если хотите настроить бота"
            )
            bot.terminateChain(it.chat.id)
        } else {
            sendMessage(
                it.chat.id, "Это чат бот, который будет отправлять расписание в ваш чат"
            )
            sendMessage(
                it.chat.id, "Назовите ваш класс (например 10Д)"
            )
        }
    }.then {
        sendMessage(it.chat.id, "Полученный класс - \"${it.text}\"")
        initializeChatValues(it.chat.id, it.text!!)
    }.build()
}

/**
 * it updates schedule manually
 */
fun buildUpdateChain() {
    bot.onCommand("/update") {
        log(it.first.chat.id, "starting /update chain")
        if (!initializedBot.contains(it.first.chat.id)) {
            sendMessage(it.first.chat.id, "Вам нужно выполнить команду /run чтобы инициализировать бота")
            return@onCommand
        }
        if (updateJob[it.first.chat.id] != null) {
            updateJob[it.first.chat.id]!!.cancel()
            updateJob[it.first.chat.id] = null
        }
        updateJob[it.first.chat.id] = myCoroutine.launch {
            launchScheduleUpdateCoroutine(it.first.chat.id)
        }
    }
}

/**
 * it creates /configure chain (command), which should be executed to configure the bot
 */
fun buildConfigureChain() {
    var stringState = 0
    bot.chain("/configure") {
        log(it.chat.id, "starting /configure chain")
        if (!initializedBot.contains(it.chat.id)) {
            sendMessage(it, "Вам нужно выполнить команду /run чтобы инициализировать бота")
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
        when (it.text) {
            "stop", "стоп" ->
                bot.terminateChain(it.chat.id)

            "class", "класс" -> {
                sendMessage(it.chat.id, "Назовите ваш класс (например 10Д)")
                stringState = 0
            }

            "link", "ссылка" -> {
                sendMessage(
                    it.chat.id,
                    "Введите ссылку на расписание (например $defaultLink)"
                )
                stringState = 1
            }

            else -> {
                sendMessage(it.chat.id, "Бот не смог распознать вашу команду")
                bot.terminateChain(it.chat.id)
            }
        }
    }.then {
        when (stringState) {
            0 -> {
                it.text!!.checkClass().let { result ->
                    if (result == null)
                        sendMessage(it.chat.id, "Не правильный формат ввода класса")
                    else {
                        sendMessage(it.chat.id, "Класс успешно обновлён")
                        chosenClass[it.chat.id] = result
                    }
                }
            }

            1 -> {
                it.text!!.checkLink().let { result ->
                    if (result == null)
                        sendMessage(it.chat.id, "Не правильная ссылка")
                    else {
                        sendMessage(it.chat.id, "Ссылка обновлена")
                        chosenLink[it.chat.id] = result
                    }
                }
            }
        }
        storeConfigs(it.chat.id, chosenClass[it.chat.id]!!, chosenLink[it.chat.id]!!, updateTime[it.chat.id]!!)
    }.build()
}