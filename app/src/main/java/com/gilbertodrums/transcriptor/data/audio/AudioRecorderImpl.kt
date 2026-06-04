package com.gilbertodrums.transcriptor.data.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioRecorderImpl : AudioRecorder {

    companion object {
        private const val SAMPLE_RATE = 16_000
        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var audioRecord: AudioRecord? = null
    private var wavWriter: WavWriter? = null
    private var recordingJob: Job? = null

    override fun startRecording(outputFile: File) {
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
            .takeIf { it > 0 } ?: return

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE, CHANNEL, ENCODING,
            bufferSize
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return
        }

        audioRecord = record
        wavWriter = WavWriter(outputFile)
        record.startRecording()

        val buffer = ShortArray(bufferSize / 2)
        recordingJob = scope.launch {
            while (isActive) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) wavWriter?.writeShorts(buffer, read)
            }
        }
    }

    override fun stopRecording() {
        recordingJob?.cancel()
        recordingJob = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        wavWriter?.close()
        wavWriter = null
    }
}
