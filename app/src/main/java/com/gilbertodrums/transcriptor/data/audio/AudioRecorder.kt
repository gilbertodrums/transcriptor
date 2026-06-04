package com.gilbertodrums.transcriptor.data.audio

import java.io.File

interface AudioRecorder {
    fun startRecording(outputFile: File)
    fun stopRecording()
}
