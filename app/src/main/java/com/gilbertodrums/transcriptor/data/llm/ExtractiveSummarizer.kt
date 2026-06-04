package com.gilbertodrums.transcriptor.data.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Plan B: extrae las frases más representativas del texto sin ningún modelo de IA.
 * Funciona en cualquier idioma y dispositivo.
 */
class ExtractiveSummarizer : Summarizer {

    override suspend fun summarize(text: String): Summary = withContext(Dispatchers.Default) {
        val sentences = text
            .split(Regex("[.!?]+\\s+|[.!?]+$"))
            .map { it.trim() }
            .filter { it.length in 20..300 }

        if (sentences.isEmpty()) {
            return@withContext Summary(
                points = listOf(text.take(200).trimEnd()),
                isAi = false
            )
        }

        val wordFreq = buildWordFrequency(text)

        val scored = sentences.mapIndexed { index, sentence ->
            val ratio = index.toFloat() / sentences.size
            // Las frases al inicio y al final suelen contener las ideas clave
            val positionScore = if (ratio < 0.2f || ratio > 0.8f) 1.4f else 1.0f
            val freqScore = sentence.lowercase()
                .split(Regex("\\W+"))
                .filter { it.length > 3 }
                .sumOf { (wordFreq[it] ?: 0).toDouble() }
                .toFloat()
            Triple(index, sentence, positionScore * (1f + freqScore * 0.01f))
        }

        val top = scored
            .sortedByDescending { it.third }
            .take(5)
            .sortedBy { it.first }   // restaurar orden de aparición
            .map { it.second }

        Summary(points = top, isAi = false)
    }

    private fun buildWordFrequency(text: String): Map<String, Int> =
        text.lowercase()
            .split(Regex("\\W+"))
            .filter { it.length > 3 }
            .groupingBy { it }
            .eachCount()
}
