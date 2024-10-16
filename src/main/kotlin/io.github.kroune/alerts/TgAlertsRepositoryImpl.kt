package io.github.kroune.alerts

import eu.vendeli.tgbot.api.message.message
import io.github.kroune.ADMIN_TG_CHAT_ID
import io.github.kroune.bot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Sends alerts to the telegram chat with me
 */
class TgAlertsRepositoryImpl: AlertsRepositoryI {
    override fun alert(message: String): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            println(message)
            message { message }.send(ADMIN_TG_CHAT_ID, bot)
        }
    }
}
