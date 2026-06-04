package com.gilbertodrums.transcriptor.data.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Resumidor basado en Gemma 3 1B (4-bit cuantizado, ~529 MB).
 * El modelo se carga desde getExternalFilesDir() — copiarlo con ADB:
 *   adb push gemma3-1b-it-int4.task /sdcard/Android/data/com.gilbertodrums.transcriptor/files/
 * Si el modelo no está o falla, delega a ExtractiveSummarizer (Plan B).
 */
class MediaPipeSummarizer(
    private val context: Context,
    private val fallback: Summarizer = ExtractiveSummarizer()
) : Summarizer {

    companion object {
        const val MODEL_FILENAME = "gemma3-1b-it-int4.task"
        private const val MAX_INPUT_CHARS = 3_000
        private const val MAX_OUTPUT_TOKENS = 512
    }

    private var llm: LlmInference? = null

    fun modelFile(): File? {
        val f = File(context.getExternalFilesDir(null), MODEL_FILENAME)
        return if (f.exists()) f else null
    }

    fun isModelAvailable(): Boolean = modelFile() != null

    /** Carga el modelo en memoria (operación lenta, ~10-30 s). */
    suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        val file = modelFile() ?: return@withContext false
        runCatching {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(file.absolutePath)
                .setMaxTokens(MAX_OUTPUT_TOKENS)
                .build()
            llm = LlmInference.createFromOptions(context, options)
            true
        }.getOrDefault(false)
    }

    override suspend fun summarize(text: String): Summary {
        val inference = llm ?: return fallback.summarize(text)
        return runCatching { generateSummary(inference, text) }
            .getOrElse { fallback.summarize(text) }
    }

    private suspend fun generateSummary(inference: LlmInference, text: String): Summary =
        withContext(Dispatchers.Default) {
            val truncated = text.take(MAX_INPUT_CHARS)
            val prompt = buildPrompt(truncated)
            val response = inference.generateResponse(prompt)
            parsePoints(response)
        }

    private fun buildPrompt(text: String) = """
        <start_of_turn>user
        Eres un asistente que resume transcripciones de clases universitarias en español. Resume el siguiente texto en 3 a 5 puntos clave, usando viñetas que empiecen con "•". Sé conciso y claro.

        TRANSCRIPCIÓN:
        $text
        <end_of_turn>
        <start_of_turn>model
    """.trimIndent()

    private fun parsePoints(response: String): Summary {
        val points = response.lines()
            .map { it.trim() }
            .filter { it.startsWith("•") || (it.startsWith("-") && it.length > 3) }
            .map { it.removePrefix("•").removePrefix("-").trim() }
            .filter { it.isNotBlank() }
            .take(5)

        return if (points.isEmpty()) {
            // El modelo respondió sin viñetas — tomamos líneas con contenido
            val lines = response.lines()
                .map { it.trim() }
                .filter { it.length > 15 }
                .take(5)
            Summary(points = lines.ifEmpty { listOf(response.trim().take(300)) }, isAi = true)
        } else {
            Summary(points = points, isAi = true)
        }
    }

    fun release() {
        llm?.close()
        llm = null
    }
}
