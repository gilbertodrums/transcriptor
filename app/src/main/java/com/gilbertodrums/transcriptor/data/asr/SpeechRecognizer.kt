package com.gilbertodrums.transcriptor.data.asr

import java.io.File

interface SpeechRecognizer {
    suspend fun transcribe(audioFile: File): String
    fun release()
}
