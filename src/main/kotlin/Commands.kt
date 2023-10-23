import com.elbekd.bot.feature.chain.chain
import com.elbekd.bot.feature.chain.terminateChain

/**
 * it creates /run chain (command), which should be executed to start the bot
 */
fun buildRunChain() {
    bot.chain("/run") {
        if (updateJob[it.chat.id] != null) {
            bot.sendMessage(it, "Вы уже запускали бота, используйте команду /configure если хотите настроить бота")
            bot.terminateChain(it.chat.id)
        } else {
            bot.sendMessage(
                it, "Это чат бот, который будет отправлять расписание в ваш чат"
            )
            bot.sendMessage(
                it, "Назовите ваш класс (например 10Д)"
            )
        }
    }.then {
        bot.sendMessage(it, "Полученный класс - \"${it.text}\"")
        initializeChatValues(it, it.text!!)
    }.build()
}

/**
 * it updates schedule manually
 */
fun buildUpdateChain() {
    bot.onCommand("/update") {
        if (updateJob[it.first.chat.id] != null) {
            updateJob[it.first.chat.id]!!.cancel()
            updateJob[it.first.chat.id] = null
        }
        launchScheduleUpdateCoroutine(it.first)
    }
}

/**
 * it creates /configure chain (command), which should be executed to configure the bot
 */
fun buildConfigureChain() {
    var stringState = 0
    bot.chain("/configure") {
        bot.sendMessage(
            it, """
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
                bot.sendMessage(it, "Назовите ваш класс (например 10Д)")
                stringState = 0
            }

            "link", "ссылка" -> {
                bot.sendMessage(
                    it,
                    "Введите ссылку на расписание (например https://docs.google.com/spreadsheets/d/1L9UjNOZx4p4VER11SCyU97M07QnfWsZWwldAAOR0gtM)"
                )
                stringState = 1
            }

            else -> {
                bot.sendMessage(it, "Бот не смог распознать вашу команду")
                bot.terminateChain(it.chat.id)
            }
        }
    }.then {
        when (stringState) {
            0 -> {
                // TODO: check if class name is valid
                chosenClass[it.chat.id] = it.text!!
                bot.sendMessage(it, "Класс успешно обновлён")
            }

            1 -> {
                it.text!!.checkLink().let { result ->
                    if (result == null)
                        bot.sendMessage(it, "Не правильная ссылка")
                    else {
                        bot.sendMessage(it, "Ссылка обновлена")
                        chosenLink[it.chat.id] = result
                    }
                }
            }
        }
    }.build()
}