package com.gilbertodrums.transcriptor.data.asr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File

class VoskSpeechRecognizer(private val modelDir: File) : SpeechRecognizer {

    // El modelo es costoso de cargar; se reutiliza entre transcripciones.
    private var model: Model? = null

    override suspend fun transcribe(audioFile: File): String = withContext(Dispatchers.IO) {
        val m = model ?: Model(modelDir.absolutePath).also { model = it }

        Recognizer(m, 16_000f).use { rec ->
            audioFile.inputStream().use { fis ->
                fis.skip(44L) // saltar cabecera WAV (siempre 44 bytes en nuestros archivos)
                val buf = ByteArray(4096)
                while (true) {
                    val n = fis.read(buf)
                    if (n < 0) break
                    rec.acceptWaveForm(buf, n)
                }
            }
            JSONObject(rec.finalResult).optString("text", "")
        }
    }

    override fun release() {
        model?.close()
        model = null
    }
}
