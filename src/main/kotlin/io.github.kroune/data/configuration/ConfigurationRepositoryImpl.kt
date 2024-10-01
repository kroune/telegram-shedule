package data.configuration

import Schedule.currentSchedule
import data.defaultOutputMode
import data.unparsedScheduleParser.ClassName
import data.updater.UpdateI
import eu.vendeli.tgbot.types.chat.Chat
import jsonClient
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File

/**
 * Default implementation of [ConfigurationRepositoryI].
 */
class ConfigurationRepositoryImpl : ConfigurationRepositoryI {
    private var classNameToChats: MutableMap<ClassName, MutableMap<Chat, Boolean>> = hashMapOf()
    private var chatToClassName: MutableMap<Chat, ClassName> = hashMapOf()
    private var outputModes: MutableMap<Chat, UpdateI> = hashMapOf()
    private var oldMessageIds: MutableMap<Chat, List<Long>> = hashMapOf()

    private val configurationDirectory = "data/"

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    private class Configuration(
        val classNameToChats: MutableMap<ClassName, MutableMap<Chat, Boolean>>,
        val chatToClassName: MutableMap<Chat, ClassName>,
        val outputModes: MutableMap<Chat, UpdateI>,
        @EncodeDefault(EncodeDefault.Mode.ALWAYS)
        val oldMessageIds: MutableMap<Chat, List<Long>> = hashMapOf()
    )

    private fun save() {
        val configDir = File(configurationDirectory)
        configDir.mkdirs()
        val configFile = File(configurationDirectory, "config.json")
        if (!configFile.exists()) {
            configFile.createNewFile()
        }
        val configuration = Configuration(classNameToChats, chatToClassName, outputModes, oldMessageIds)
        val text = jsonClient.encodeToString<Configuration>(configuration)
        configFile.writeText(text)
    }

    init {
        val configDir = File(configurationDirectory)
        configDir.mkdirs()
        val configFile = File(configurationDirectory, "config.json")
        if (configFile.exists()) {
            jsonClient.decodeFromString<Configuration>(configFile.readText()).let {
                classNameToChats = it.classNameToChats
                chatToClassName = it.chatToClassName
                outputModes = it.outputModes
                oldMessageIds = it.oldMessageIds
            }
        }
    }

    override fun setUserWatchedClass(chat: Chat, newClassName: String) {
        getOutputMode(chat).notifyUserAboutChanges(
            chat,
            currentSchedule[getWatchedClassForChat(chat)] ?: mapOf(),
            currentSchedule[newClassName] ?: mapOf()
        )
        classNameToChats[getWatchedClassForChat(chat)]?.remove(chat)
        chatToClassName[chat] = newClassName
        classNameToChats.getOrPut(newClassName) {
            hashMapOf<Chat, Boolean>()
        }.put(chat, true)
        save()
    }

    override fun getClassWatchers(className: ClassName): List<Chat> {
        return classNameToChats[className]?.map { it.key } ?: emptyList()
    }

    override fun getWatchedClassForChat(chat: Chat): ClassName? {
        return chatToClassName[chat]
    }

    override fun setOutputMode(chat: Chat, outputMode: UpdateI) {
        outputModes[chat] = outputMode
        save()
    }

    override fun getOutputMode(chat: Chat): UpdateI {
        return outputModes[chat] ?: defaultOutputMode
    }

    override fun deleteChat(chat: Chat) {
        classNameToChats[getWatchedClassForChat(chat)]?.remove(chat)
        chatToClassName.remove(chat)
        outputModes.remove(chat)
        save()
    }

    override fun setOldMessageIds(chat: Chat, oldMessages: List<Long>) {
        oldMessageIds[chat] = oldMessages
        save()
    }

    override fun getOldMessageIds(chat: Chat): List<Long> {
        return oldMessageIds[chat] ?: emptyList()
    }
}
