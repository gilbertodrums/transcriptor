package com.gilbertodrums.transcriptor.data.asr

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Descarga y extrae el modelo Vosk en el almacenamiento privado de la app.
 * Red usada ÚNICAMENTE para esta descarga inicial; no se envía ningún dato del usuario.
 */
internal class VoskModelManager(context: Context) {

    companion object {
        private const val MODEL_URL =
            "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip"
        private const val MODEL_DIR = "vosk-model-small-es-0.42"
        private const val TMP_ZIP = "vosk-model.zip"
    }

    val modelDir: File = File(context.filesDir, MODEL_DIR)
    private val tmpZip: File = File(context.filesDir, TMP_ZIP)

    fun isAvailable(): Boolean = modelDir.exists() && modelDir.isDirectory

    suspend fun downloadAndExtract(onProgress: (Int) -> Unit) = withContext(Dispatchers.IO) {
        try {
            download(onProgress)
            extract()
        } finally {
            tmpZip.delete()
        }
    }

    private fun download(onProgress: (Int) -> Unit) {
        val conn = URL(MODEL_URL).openConnection() as HttpURLConnection
        val total = conn.contentLengthLong.takeIf { it > 0 }
        var received = 0L

        conn.inputStream.use { input ->
            FileOutputStream(tmpZip).use { out ->
                val buf = ByteArray(8192)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    received += n
                    total?.let { onProgress((received * 100 / it).toInt()) }
                }
            }
        }
    }

    private fun extract() {
        ZipInputStream(tmpZip.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val target = File(modelDir.parent!!, entry.name)
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { zis.copyTo(it) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
