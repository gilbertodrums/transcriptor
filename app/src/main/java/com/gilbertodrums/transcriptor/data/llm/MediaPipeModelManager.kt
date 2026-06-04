package com.gilbertodrums.transcriptor.data.llm

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Descarga el modelo Gemma 3 1B (int4, formato .task de MediaPipe) directamente,
 * sin token ni licencia para el usuario final: un solo botón dentro de la app.
 *
 * El modelo se sirve desde un repositorio público de HuggingFace (descarga abierta,
 * redirect al CDN sin Authorization). Solo se descarga una vez; sin datos de usuario.
 */
class MediaPipeModelManager(private val context: Context) {

    companion object {
        const val MODEL_FILENAME = "gemma3-1b-it-int4.task"

        // Mirror público (no gated) del modelo oficial litert-community/Gemma3-1B-IT.
        // Gemma 3 1B int4, contexto 2048. ~554 MB. Verificado descargable sin token.
        private const val MODEL_URL =
            "https://huggingface.co/typosbro/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task"

        // Tamaño esperado para validar integridad de la descarga.
        private const val EXPECTED_SIZE = 554_661_246L
        private const val MIN_VALID_SIZE = 500_000_000L
    }

    val modelFile: File get() = File(context.getExternalFilesDir(null), MODEL_FILENAME)

    fun isModelAvailable(): Boolean =
        modelFile.exists() && modelFile.length() >= MIN_VALID_SIZE

    suspend fun download(onProgress: (Int) -> Unit) = withContext(Dispatchers.IO) {
        val tmpFile = File(context.getExternalFilesDir(null), "$MODEL_FILENAME.tmp")
        try {
            val conn = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 30_000
                readTimeout = 60_000
            }
            conn.connect()

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                error("HTTP ${conn.responseCode} al descargar el modelo")
            }

            val total = conn.contentLengthLong.takeIf { it > 0 } ?: EXPECTED_SIZE
            var received = 0L

            conn.inputStream.use { input ->
                FileOutputStream(tmpFile).use { out ->
                    val buf = ByteArray(262_144) // 256 KB
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        received += n
                        onProgress((received * 100 / total).toInt().coerceIn(0, 100))
                    }
                }
            }

            if (tmpFile.length() < MIN_VALID_SIZE) {
                error("Descarga incompleta (${tmpFile.length()} bytes)")
            }

            modelFile.delete()
            if (!tmpFile.renameTo(modelFile)) {
                error("No se pudo guardar el modelo")
            }
        } catch (e: Exception) {
            tmpFile.delete()
            throw e
        }
    }

    fun deleteModel() {
        modelFile.delete()
    }
}
