package com.gilbertodrums.transcriptor.ui.recording

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gilbertodrums.transcriptor.data.audio.AudioRecorderImpl
import com.gilbertodrums.transcriptor.domain.model.Recording
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class RecordingViewModel(application: Application) : AndroidViewModel(application) {

    private val audioRecorder = AudioRecorderImpl()
    private var mediaPlayer: MediaPlayer? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordings = MutableStateFlow<List<Recording>>(emptyList())
    val recordings: StateFlow<List<Recording>> = _recordings.asStateFlow()

    private val _playingId = MutableStateFlow<String?>(null)
    val playingId: StateFlow<String?> = _playingId.asStateFlow()

    private var currentFile: File? = null

    fun startRecording() {
        val file = File(
            getApplication<Application>().filesDir,
            "rec_${System.currentTimeMillis()}.wav"
        )
        currentFile = file
        audioRecorder.startRecording(file)
        _isRecording.value = true
    }

    fun stopRecording() {
        audioRecorder.stopRecording()
        _isRecording.value = false

        val file = currentFile ?: return
        currentFile = null

        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        val recording = Recording(
            id = UUID.randomUUID().toString(),
            title = LocalDateTime.now().format(formatter),
            createdAt = LocalDateTime.now(),
            filePath = file.absolutePath
        )
        _recordings.value = _recordings.value + recording
    }

    fun togglePlayback(recording: Recording) {
        viewModelScope.launch {
            if (_playingId.value == recording.id) {
                stopPlayback()
            } else {
                stopPlayback()
                runCatching {
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(recording.filePath)
                        prepare()
                        setOnCompletionListener { _playingId.value = null }
                        start()
                    }
                    _playingId.value = recording.id
                }
            }
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _playingId.value = null
    }

    override fun onCleared() {
        super.onCleared()
        if (_isRecording.value) audioRecorder.stopRecording()
        stopPlayback()
    }
}
