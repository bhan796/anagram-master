package com.bhan796.anagramarena.data

import android.content.Context
import com.bhan796.anagram.core.model.Conundrum
import com.bhan796.anagram.core.validation.DictionaryProvider
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
            context.assets.open("data/dictionary_sample.txt").bufferedReader().use(BufferedReader::readLines)
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
    val conundrumProvider: ConundrumProvider
) {
    companion object {
        fun from(context: Context): AppDependencies {
            val appContext = context.applicationContext
            return AppDependencies(
                dictionaryProvider = AssetDictionaryProvider(appContext),
                conundrumProvider = AssetConundrumProvider(appContext)
            )
        }
    }
}