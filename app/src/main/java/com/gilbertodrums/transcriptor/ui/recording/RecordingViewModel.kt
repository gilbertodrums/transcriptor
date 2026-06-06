package com.gilbertodrums.transcriptor.ui.recording

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gilbertodrums.transcriptor.data.RecordingRepository
import com.gilbertodrums.transcriptor.data.asr.VoskModelManager
import com.gilbertodrums.transcriptor.data.asr.VoskSpeechRecognizer
import com.gilbertodrums.transcriptor.data.audio.AudioRecorderImpl
import com.gilbertodrums.transcriptor.data.llm.ExtractiveSummarizer
import com.gilbertodrums.transcriptor.data.llm.MediaPipeSummarizer
import com.gilbertodrums.transcriptor.domain.model.Recording
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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
    data object NotDownloaded : LlmState               // modelo no descargado aún
    data class Downloading(val progress: Int) : LlmState
    data object Loading : LlmState                      // modelo en disco, cargando en RAM
    data object Ready : LlmState
    data class Error(val message: String) : LlmState
}

class RecordingViewModel(application: Application) : AndroidViewModel(application) {

    // — Persistencia —
    private val repository = RecordingRepository(application)

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

    // Lista persistida en Room; se actualiza sola al cambiar la BD.
    val recordings: StateFlow<List<Recording>> = repository.recordings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
            if (mediaPipeSummarizer.isModelAvailable()) loadLlm()
            else _llmState.value = LlmState.NotDownloaded
        }
    }

    private suspend fun loadLlm() {
        _llmState.value = LlmState.Loading
        val ok = mediaPipeSummarizer.loadModel()
        _llmState.value = if (ok) LlmState.Ready
        else LlmState.Error("No se pudo cargar el modelo")
    }

    fun downloadLlmModel() {
        viewModelScope.launch {
            runCatching {
                mediaPipeSummarizer.manager.download { progress ->
                    _llmState.value = LlmState.Downloading(progress)
                }
                loadLlm()
            }.onFailure { e ->
                _llmState.value = LlmState.Error(e.message ?: "Error desconocido")
            }
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
        val recording = Recording(
            id = UUID.randomUUID().toString(),
            title = LocalDateTime.now().format(formatter),
            createdAt = LocalDateTime.now(),
            filePath = file.absolutePath
        )
        viewModelScope.launch { repository.save(recording) }
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

    // — Borrado —

    fun deleteRecording(recording: Recording) {
        viewModelScope.launch {
            if (_playingId.value == recording.id) stopPlayback()
            runCatching { File(recording.filePath).delete() }
            repository.delete(recording)
        }
    }

    private suspend fun updateRecording(id: String, transform: (Recording) -> Recording) {
        val current = repository.getById(id) ?: return
        repository.save(transform(current))
    }

    override fun onCleared() {
        super.onCleared()
        if (_isRecording.value) audioRecorder.stopRecording()
        stopPlayback()
        speechRecognizer?.release()
        mediaPipeSummarizer.release()
    }
}
