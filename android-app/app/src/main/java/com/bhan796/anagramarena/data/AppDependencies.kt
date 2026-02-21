package com.bhan796.anagramarena.data

import android.content.Context
import com.bhan796.anagramarena.BuildConfig
import com.bhan796.anagram.core.model.Conundrum
import com.bhan796.anagram.core.validation.DictionaryProvider
import com.bhan796.anagramarena.network.SocketIoMultiplayerClient
import com.bhan796.anagramarena.repository.OnlineMatchRepository
import com.bhan796.anagramarena.storage.SessionStore
import java.io.BufferedReader
import kotlin.random.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class AssetDictionaryProvider(context: Context) : DictionaryProvider {
    private val words: Set<String> = loadWords(context)

    override fun contains(normalizedWord: String): Boolean = words.contains(normalizedWord)

    fun isLoaded(): Boolean = words.isNotEmpty()

    private fun loadWords(context: Context): Set<String> {
        return runCatching {
            context.assets.open("data/dictionary_common_10k.txt").bufferedReader().use(BufferedReader::readLines)
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() && it.all(Char::isLetter) }
                .toSet()
        }.getOrElse { emptySet() }
    }
}

interface ConundrumProvider {
    fun randomConundrum(): Conundrum?
    fun allConundrums(): List<Conundrum>
}

class AssetConundrumProvider(context: Context) : ConundrumProvider {
    private val conundrums: List<Conundrum> = loadConundrums(context)

    override fun randomConundrum(): Conundrum? = conundrums.randomOrNull(Random.Default)

    override fun allConundrums(): List<Conundrum> = conundrums

    private fun loadConundrums(context: Context): List<Conundrum> {
        return runCatching {
            val raw = context.assets.open("data/conundrums.json").bufferedReader().use { it.readText() }
            Json.decodeFromString<List<ConundrumDto>>(raw).map { dto ->
                Conundrum(id = dto.id, scrambled = dto.scrambled, answer = dto.answer)
            }
        }.getOrElse { emptyList() }
    }
}

@Serializable
private data class ConundrumDto(
    val id: String,
    val scrambled: String,
    val answer: String
)

data class AppDependencies(
    val dictionaryProvider: AssetDictionaryProvider,
    val conundrumProvider: ConundrumProvider,
    val onlineMatchRepository: OnlineMatchRepository
) {
    companion object {
        fun from(context: Context): AppDependencies {
            val appContext = context.applicationContext
            val sessionStore = SessionStore(appContext)
            val socketClient = SocketIoMultiplayerClient()

            return AppDependencies(
                dictionaryProvider = AssetDictionaryProvider(appContext),
                conundrumProvider = AssetConundrumProvider(appContext),
                onlineMatchRepository = OnlineMatchRepository(
                    socketClient = socketClient,
                    sessionStore = sessionStore,
                    backendUrl = BuildConfig.BACKEND_BASE_URL
                )
            )
        }
    }
}
