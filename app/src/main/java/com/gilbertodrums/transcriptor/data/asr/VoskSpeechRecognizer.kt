package com.gilbertodrums.transcriptor.data.asr

import android.os.Process
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.util.concurrent.Executors

class VoskSpeechRecognizer(private val modelDir: File) : SpeechRecognizer {

    // El modelo es costoso de cargar; se reutiliza entre transcripciones.
    private var model: Model? = null

    // Hilo único y de baja prioridad: la transcripción corre aquí, cediendo CPU a la UI
    // (evita el "no responde" con audios largos).
    private val transcribeDispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            runnable.run()
        }, "vosk-transcribe")
    }.asCoroutineDispatcher()

    override suspend fun transcribe(audioFile: File): String = withContext(transcribeDispatcher) {
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
        transcribeDispatcher.close()
    }
}
