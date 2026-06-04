package com.gilbertodrums.transcriptor.ui.recording

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gilbertodrums.transcriptor.data.asr.VoskModelManager
import com.gilbertodrums.transcriptor.data.asr.VoskSpeechRecognizer
import com.gilbertodrums.transcriptor.data.audio.AudioRecorderImpl
import com.gilbertodrums.transcriptor.data.llm.ExtractiveSummarizer
import com.gilbertodrums.transcriptor.data.llm.MediaPipeSummarizer
import com.gilbertodrums.transcriptor.domain.model.Recording
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

sealed interface ModelState {
    data object Checking : ModelState
    data object NotDownloaded : ModelState
    data class Downloading(val progress: Int) : ModelState
    data object Ready : ModelState
    data class Error(val message: String) : ModelState
}

sealed interface LlmState {
    data object Checking : LlmState
    data object ModelNotFound : LlmState
    data object Loading : LlmState
    data object Ready : LlmState
    data class Error(val message: String) : LlmState
}

class RecordingViewModel(application: Application) : AndroidViewModel(application) {

    // — ASR (Vosk) —
    private val audioRecorder = AudioRecorderImpl()
    private val modelManager = VoskModelManager(application)
    private var speechRecognizer: VoskSpeechRecognizer? = null

    // — Sumario —
    private val mediaPipeSummarizer = MediaPipeSummarizer(application)
    private val extractiveSummarizer = ExtractiveSummarizer()

    // — Audio —
    private var mediaPlayer: MediaPlayer? = null
    private var currentFile: File? = null

    // — Estados —
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordings = MutableStateFlow<List<Recording>>(emptyList())
    val recordings: StateFlow<List<Recording>> = _recordings.asStateFlow()

    private val _playingId = MutableStateFlow<String?>(null)
    val playingId: StateFlow<String?> = _playingId.asStateFlow()

    private val _modelState = MutableStateFlow<ModelState>(ModelState.Checking)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    private val _llmState = MutableStateFlow<LlmState>(LlmState.Checking)
    val llmState: StateFlow<LlmState> = _llmState.asStateFlow()

    private val _transcribingId = MutableStateFlow<String?>(null)
    val transcribingId: StateFlow<String?> = _transcribingId.asStateFlow()

    private val _summarizingId = MutableStateFlow<String?>(null)
    val summarizingId: StateFlow<String?> = _summarizingId.asStateFlow()

    init {
        checkVoskModel()
        checkLlmModel()
    }

    // — Vosk —

    private fun checkVoskModel() {
        viewModelScope.launch {
            _modelState.value = if (modelManager.isAvailable()) {
                speechRecognizer = VoskSpeechRecognizer(modelManager.modelDir)
                ModelState.Ready
            } else {
                ModelState.NotDownloaded
            }
        }
    }

    fun downloadModel() {
        viewModelScope.launch {
            runCatching {
                modelManager.downloadAndExtract { progress ->
                    _modelState.value = ModelState.Downloading(progress)
                }
                speechRecognizer = VoskSpeechRecognizer(modelManager.modelDir)
                _modelState.value = ModelState.Ready
            }.onFailure { e ->
                _modelState.value = ModelState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    // — Gemma / MediaPipe —

    private fun checkLlmModel() {
        viewModelScope.launch {
            if (!mediaPipeSummarizer.isModelAvailable()) {
                _llmState.value = LlmState.ModelNotFound
                return@launch
            }
            _llmState.value = LlmState.Loading
            val ok = mediaPipeSummarizer.loadModel()
            _llmState.value = if (ok) LlmState.Ready else LlmState.Error("No se pudo cargar el modelo")
        }
    }

    // — Transcripción —

    fun transcribe(recording: Recording) {
        val recognizer = speechRecognizer ?: return
        viewModelScope.launch {
            _transcribingId.value = recording.id
            runCatching {
                val text = recognizer.transcribe(File(recording.filePath))
                updateRecording(recording.id) { it.copy(transcript = text) }
            }
            _transcribingId.value = null
        }
    }

    // — Resumen —

    fun summarize(recording: Recording) {
        val text = recording.transcript ?: return
        viewModelScope.launch {
            _summarizingId.value = recording.id
            runCatching {
                val summary = if (_llmState.value == LlmState.Ready) {
                    mediaPipeSummarizer.summarize(text)
                } else {
                    extractiveSummarizer.summarize(text)
                }
                updateRecording(recording.id) { it.copy(summary = summary) }
            }
            _summarizingId.value = null
        }
    }

    // — Grabación —

    fun startRecording() {
        val file = File(getApplication<Application>().filesDir, "rec_${System.currentTimeMillis()}.wav")
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
        _recordings.value = _recordings.value + Recording(
            id = UUID.randomUUID().toString(),
            title = LocalDateTime.now().format(formatter),
            createdAt = LocalDateTime.now(),
            filePath = file.absolutePath
        )
    }

    // — Reproducción —

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

    private fun updateRecording(id: String, transform: (Recording) -> Recording) {
        _recordings.value = _recordings.value.map { if (it.id == id) transform(it) else it }
    }

    override fun onCleared() {
        super.onCleared()
        if (_isRecording.value) audioRecorder.stopRecording()
        stopPlayback()
        speechRecognizer?.release()
        mediaPipeSummarizer.release()
    }
}
