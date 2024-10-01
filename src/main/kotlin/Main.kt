import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.types.internal.HttpLogLevel
import eu.vendeli.tgbot.types.internal.LogLvl
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

private val token = System.getenv("BOT_TOKEN")!!

/**
 * our telegram bot
 */
val bot = TelegramBot(token) {
    this.commandParsing {
        this.useIdentifierInGroupCommands = true
    }
    this.logging {
        this.botLogLevel = LogLvl.ALL
        this.httpLogLevel = HttpLogLevel.BODY
    }
}

/**
 * json client we use
 */
val jsonClient = Json {
    prettyPrint = true
    allowStructuredMapKeys = true
}

/**
 * App entry point. Starts schedule updating and starts telegram bot
 */
fun main() {
    runBlocking {
        Schedule
        bot.handleUpdates()
    }
}
