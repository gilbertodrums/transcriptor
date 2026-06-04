package com.gilbertodrums.transcriptor.data.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Plan B: divide el texto en bloques de ~40 palabras, puntúa cada bloque por densidad
 * de palabras clave (ignorando stopwords) y devuelve los más representativos.
 * Diseñado para la salida de Vosk, que carece de puntuación.
 */
class ExtractiveSummarizer : Summarizer {

    override suspend fun summarize(text: String): Summary = withContext(Dispatchers.Default) {
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

        if (words.size < 20) {
            return@withContext Summary(points = listOf(text.trim()), isAi = false)
        }

        // Bloques de 40 palabras (~15 s de habla a ritmo normal)
        val chunks = words.chunked(CHUNK_SIZE).map { it.joinToString(" ") }

        // Frecuencia de palabras clave (excluye stopwords)
        val freq = words
            .map { normalize(it) }
            .filter { it.length > 3 && it !in STOPWORDS }
            .groupingBy { it }
            .eachCount()

        // Puntuación por densidad + bonus posicional (intro y cierre)
        val scored = chunks.mapIndexed { i, chunk ->
            val chunkWords = chunk.split(Regex("\\s+"))
            val density = chunkWords
                .map { normalize(it) }
                .filter { it.length > 3 && it !in STOPWORDS }
                .sumOf { freq.getOrDefault(it, 0).toDouble() } / chunkWords.size.coerceAtLeast(1)
            val posBonus = if (i < 2 || i >= chunks.size - 2) 1.35 else 1.0
            i to (density * posBonus)
        }

        // Número de puntos proporcional al largo (mín 3, máx 5)
        val targetCount = (chunks.size / 3).coerceIn(3, 5)

        val points = scored
            .sortedByDescending { it.second }
            .take(targetCount)
            .sortedBy { it.first }               // restaurar orden cronológico
            .map { (idx, _) -> truncate(chunks[idx]) }

        Summary(points = points, isAi = false)
    }

    private fun normalize(word: String): String =
        word.lowercase().replace(Regex("[^a-záéíóúüñ]"), "")

    private fun truncate(chunk: String): String {
        val words = chunk.split(" ")
        return if (words.size > MAX_POINT_WORDS)
            words.take(MAX_POINT_WORDS).joinToString(" ") + "…"
        else chunk
    }

    companion object {
        private const val CHUNK_SIZE = 40
        private const val MAX_POINT_WORDS = 35

        private val STOPWORDS = setOf(
            // Español
            "que", "de", "en", "el", "la", "los", "las", "un", "una", "unos", "unas",
            "y", "o", "a", "con", "por", "para", "del", "al", "se", "es", "son", "fue",
            "como", "mas", "pero", "si", "ya", "lo", "le", "me", "te", "nos", "esto",
            "este", "esta", "estos", "estas", "hay", "ser", "estar", "tener", "hacer",
            "también", "muy", "bien", "cuando", "donde", "entonces", "porque", "sobre",
            "todo", "cada", "entre", "hasta", "desde", "durante", "después", "antes",
            "aquí", "allí", "así", "tal", "cual", "cuyo", "cuya", "eso", "esa", "esos",
            "esas", "ese", "nos", "ellos", "ellas", "yo", "usted", "vamos", "mente",
            // Inglés (por si la transcripción mezcla)
            "the", "and", "for", "are", "but", "not", "you", "all", "can", "that",
            "this", "with", "have", "from", "they", "was", "his", "her", "been"
        )
    }
}
