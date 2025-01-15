package io.github.kroune.configuration

import eu.vendeli.tgbot.types.chat.Chat
import io.github.kroune.CONFIGURATION_DIRECTORY
import io.github.kroune.defaultOutputMode
import io.github.kroune.jsonClient
import io.github.kroune.retryableExitedOnFatal
import io.github.kroune.unparsedScheduleParser.ClassName
import io.github.kroune.updater.UpdateI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.emptyList
import kotlin.collections.get
import kotlin.collections.getOrPut
import kotlin.collections.hashMapOf
import kotlin.collections.map
import kotlin.collections.set

/**
 * Default implementation of [ConfigurationRepositoryI].
 */
class ConfigurationRepositoryImpl : ConfigurationRepositoryI {
    private var classNameToChats: MutableMap<ClassName, MutableMap<Chat, Boolean>> = hashMapOf()
    private var chatToClassName: MutableMap<Chat, ClassName> = hashMapOf()
    private var outputModes: MutableMap<Chat, UpdateI> = hashMapOf()
    private var oldMessageIds: MutableMap<Chat, List<Long>> = hashMapOf()

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    private class Configuration(
        val classNameToChats: MutableMap<ClassName, MutableMap<Chat, Boolean>>,
        val chatToClassName: MutableMap<Chat, ClassName>,
        val outputModes: MutableMap<Chat, UpdateI>,
        @EncodeDefault(EncodeDefault.Mode.ALWAYS)
        val oldMessageIds: MutableMap<Chat, List<Long>> = hashMapOf()
    )

    override fun getChats(): List<Chat> {
        return chatToClassName.map { it.key }
    }

    private fun save() {
        {
            val configDir = File(CONFIGURATION_DIRECTORY)
            configDir.mkdirs()
            val configFile = File(CONFIGURATION_DIRECTORY, "config.json")
            if (!configFile.exists()) {
                configFile.createNewFile()
            }
            val configuration = Configuration(classNameToChats, chatToClassName, outputModes, oldMessageIds)
            val text = jsonClient.encodeToString<Configuration>(configuration)
            configFile.writeText(text)
        }.retryableExitedOnFatal()
    }

    init {
        {
            val configDir = File(CONFIGURATION_DIRECTORY)
            configDir.mkdirs()
            val configFile = File(CONFIGURATION_DIRECTORY, "config.json")
            if (configFile.exists()) {
                jsonClient.decodeFromString<Configuration>(configFile.readText()).let {
                    classNameToChats = it.classNameToChats
                    chatToClassName = it.chatToClassName
                    outputModes = it.outputModes
                    oldMessageIds = it.oldMessageIds
                }
            }
        }.retryableExitedOnFatal()
    }

    override fun setUserWatchedClass(chat: Chat, newClassName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            getOutputMode(chat).notifyUserAboutChanges(chat)
        }
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
        oldMessageIds.remove(chat)
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
