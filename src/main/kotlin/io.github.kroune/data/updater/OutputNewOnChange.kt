package data.updater

import Notifier.transformToMessage
import bot
import data.unparsedScheduleParser.Lessons
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.chat.Chat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.serialization.Serializable

/**
 * outputs schedule, when it changes, ignoring previous schedule
 */
@Serializable
class OutputNewOnChange: UpdateI {
    override val modeName: String = "OutputNewOnChange"

    override fun notifyUserAboutChanges(
        chat: Chat,
        oldSchedule: Map<DayOfWeek, Lessons>,
        newSchedule: Map<DayOfWeek, Lessons>
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            transformToMessage(newSchedule).forEach {
                message(it).send(chat, bot)
            }
        }
    }
}
