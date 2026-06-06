package com.gilbertodrums.transcriptor.data.llm

import android.content.Context
import android.os.Process
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.concurrent.Executors

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

        // setMaxTokens es el presupuesto TOTAL (entrada + salida). Debe caber el prompt
        // completo MÁS la respuesta; si la entrada lo desborda, la generación se vuelve
        // lentísima/inestable (causa del ANR con textos largos).
        private const val MAX_TOTAL_TOKENS = 1_280
        // Truncamos la entrada para dejar SIEMPRE margen de salida dentro del presupuesto.
        // ~2000 caracteres ≈ 520 tokens; +plantilla deja ~700 tokens para la respuesta.
        private const val MAX_INPUT_CHARS = 2_000
        // Si la IA tarda más que esto, cancelamos y caemos al resumen básico (la UI no se cuelga).
        private const val GENERATION_TIMEOUT_MS = 45_000L
    }

    private var llm: LlmInference? = null

    val manager = MediaPipeModelManager(context)

    // Hilo único y de baja prioridad para la inferencia: serializa las generaciones
    // (evita que se acumulen y disparen un OOM) y cede CPU al hilo de la interfaz.
    private val inferenceDispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            runnable.run()
        }, "gemma-inference")
    }.asCoroutineDispatcher()

    fun isModelAvailable(): Boolean = manager.isModelAvailable()

    /** Carga el modelo en memoria (operación lenta, ~10-30 s). */
    suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        if (!manager.isModelAvailable()) return@withContext false
        runCatching {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(manager.modelFile.absolutePath)
                .setMaxTokens(MAX_TOTAL_TOKENS)
                .build()
            llm = LlmInference.createFromOptions(context, options)
            true
        }.getOrDefault(false)
    }

    override suspend fun summarize(text: String): Summary {
        val inference = llm ?: return fallback.summarize(text)
        return try {
            withTimeout(GENERATION_TIMEOUT_MS) { generateSummary(inference, text) }
        } catch (e: TimeoutCancellationException) {
            // La IA tardó demasiado: devolvemos el resumen básico en vez de colgar la app.
            fallback.summarize(text)
        } catch (e: CancellationException) {
            throw e // cancelación real (p. ej. ViewModel destruido): propagar
        } catch (e: Exception) {
            fallback.summarize(text)
        }
    }

    private suspend fun generateSummary(inference: LlmInference, text: String): Summary =
        withContext(inferenceDispatcher) {
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
        inferenceDispatcher.close()
    }
}
