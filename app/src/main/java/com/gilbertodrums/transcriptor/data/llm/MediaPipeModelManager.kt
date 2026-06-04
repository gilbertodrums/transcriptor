package com.gilbertodrums.transcriptor.data.llm

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Descarga el modelo Gemma 3 1B desde HuggingFace usando el token del usuario.
 * El token se guarda en SharedPreferences (solo en el dispositivo, nunca en el repo).
 * Requiere que el usuario haya aceptado la licencia Gemma en huggingface.co/google/gemma-3-1b-it
 */
class MediaPipeModelManager(private val context: Context) {

    companion object {
        const val MODEL_FILENAME = "gemma3-1b-it-int4.task"
        private const val PREFS_NAME = "llm_prefs"
        private const val KEY_HF_TOKEN = "hf_token"
        private const val MODEL_URL =
            "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task"
    }

    val modelFile: File get() = File(context.getExternalFilesDir(null), MODEL_FILENAME)

    fun isModelAvailable(): Boolean = modelFile.exists()

    fun savedToken(): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_HF_TOKEN, null)
            ?.takeIf { it.isNotBlank() }

    fun saveToken(token: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_HF_TOKEN, token.trim()).apply()
    }

    fun clearToken() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_HF_TOKEN).apply()
    }

    suspend fun download(token: String, onProgress: (Int) -> Unit) = withContext(Dispatchers.IO) {
        val tmpFile = File(context.getExternalFilesDir(null), "$MODEL_FILENAME.tmp")
        try {
            val conn = URL(MODEL_URL).openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.connect()

            if (conn.responseCode == 401 || conn.responseCode == 403) {
                error("Token inválido o licencia no aceptada (${conn.responseCode})")
            }
            if (conn.responseCode != 200) {
                error("Error al descargar: HTTP ${conn.responseCode}")
            }

            val total = conn.contentLengthLong.takeIf { it > 0 }
            var received = 0L

            conn.inputStream.use { input ->
                FileOutputStream(tmpFile).use { out ->
                    val buf = ByteArray(131_072) // 128 KB
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        received += n
                        total?.let { onProgress((received * 100 / it).toInt()) }
                    }
                }
            }
            tmpFile.renameTo(modelFile)
        } catch (e: Exception) {
            tmpFile.delete()
            throw e
        }
    }
}
