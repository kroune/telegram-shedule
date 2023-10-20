import com.elbekd.bot.feature.chain.chain
import com.elbekd.bot.feature.chain.terminateChain

fun buildRunChain() {
    bot.chain("/run") {
        if (updateJob != null) {
            println("this bot is already running")
            bot.sendMessage(it, "Вы уже запускали бота, используйте команду /configure если хотите настроить бота")
            bot.terminateChain(it.chat.id)
        } else {
            println("first run")
            bot.sendMessage(
                it, "Это чат бот, который будет отправлять расписание в ваш чат"
            )
            bot.sendMessage(
                it, "Назовите ваш класс (например 10Д)"
            )
        }
    }.then {
        println("class - ${it.text}")
        bot.sendMessage(it, "Полученный класс - \"${it.text}\"")
        chosenClass = it.text!!
        start(it)
    }.build()
}

fun buildConfigureChain() {
    var stringState = 0
    bot.chain("/configure") {
        println("configuring")
        bot.sendMessage(
            it,
            """
                Список настроек: {
                    'класс'
                    'ссылка' (ссылка на расписание)
                    'обновить' (принудительное обновление расписания) 
                }
                выведите 'стоп' если хотите завершить настройку     
            """.trimMargin()
        )
    }.then {
        when (it.text) {
            "stop", "стоп" -> {
                println("stop")
                bot.terminateChain(it.chat.id)
            }

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

            "update", "обновить" -> {
                bot.sendMessage(it, "Обновляем...")
                println("updating")
                stringState = 2
            }

            else -> {
                bot.sendMessage(it, "Бот не смог распознать вашу команду")
                bot.terminateChain(it.chat.id)
            }
        }
    }.then {
        when (stringState) {
            0 -> {
                println("class ${it.text}")
                chosenClass = it.text!!
                bot.sendMessage(it, "Класс успешно обновлён")
            }

            1 -> {
                println("link ${it.text}")
                it.text!!.checkLink().let { result ->
                    if (result == null) {
                        bot.sendMessage(it, "Не правильная ссылка")
                    } else {
                        bot.sendMessage(it, "Ссылка обновлена")
                        chosenLink = result
                    }
                }
            }

            2 -> {
                if (updateJob != null) {
                    updateJob!!.cancel()
                    updateJob = null
                }
                start(it)
            }
        }
    }.build()
}