package com.gilbertodrums.transcriptor.ui.recording

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gilbertodrums.transcriptor.R
import com.gilbertodrums.transcriptor.domain.model.Recording

@Composable
fun RecordingScreen(vm: RecordingViewModel = viewModel()) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    val isRecording by vm.isRecording.collectAsState()
    val recordings by vm.recordings.collectAsState()
    val playingId by vm.playingId.collectAsState()
    val modelState by vm.modelState.collectAsState()
    val transcribingId by vm.transcribingId.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        // — Permiso de micrófono —
        if (!hasPermission) {
            PermissionSection(onRequest = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) })
            Spacer(Modifier.height(16.dp))
        }

        // — Estado del modelo ASR —
        ModelSection(state = modelState, onDownload = vm::downloadModel)

        Spacer(Modifier.height(24.dp))

        // — Botón Grabar (solo si tiene permiso) —
        if (hasPermission) {
            RecordButton(
                isRecording = isRecording,
                onToggle = { if (isRecording) vm.stopRecording() else vm.startRecording() }
            )
        }

        Spacer(Modifier.height(24.dp))

        // — Lista de grabaciones —
        if (recordings.isEmpty()) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.no_recordings),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = stringResource(R.string.recordings_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(recordings, key = { it.id }) { recording ->
                    RecordingItem(
                        recording = recording,
                        isPlaying = playingId == recording.id,
                        isTranscribing = transcribingId == recording.id,
                        canTranscribe = modelState is ModelState.Ready,
                        onTogglePlay = { vm.togglePlayback(recording) },
                        onTranscribe = { vm.transcribe(recording) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelSection(state: ModelState, onDownload: () -> Unit) {
    when (state) {
        ModelState.Checking -> Unit
        ModelState.NotDownloaded -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.model_not_downloaded),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onDownload) {
                        Text(stringResource(R.string.download_model))
                    }
                }
            }
        }
        is ModelState.Downloading -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.model_downloading, state.progress),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        ModelState.Ready -> Unit
        is ModelState.Error -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.model_error, state.message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onDownload) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionSection(onRequest: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.permission_rationale),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRequest) {
            Text(stringResource(R.string.grant_permission))
        }
    }
}

@Composable
private fun RecordButton(isRecording: Boolean, onToggle: () -> Unit) {
    Button(
        onClick = onToggle,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isRecording) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary
        )
    ) {
        Text(stringResource(if (isRecording) R.string.stop_recording else R.string.start_recording))
    }
    if (isRecording) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.recording_in_progress),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun RecordingItem(
    recording: Recording,
    isPlaying: Boolean,
    isTranscribing: Boolean,
    canTranscribe: Boolean,
    onTogglePlay: () -> Unit,
    onTranscribe: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = recording.title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onTogglePlay) {
                    Text(stringResource(if (isPlaying) R.string.stop_playback else R.string.play))
                }
            }

            // — Transcripción —
            when {
                isTranscribing -> {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        Text(
                            text = stringResource(R.string.transcribing),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                recording.transcript != null -> {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = recording.transcript,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                canTranscribe -> {
                    TextButton(onClick = onTranscribe) {
                        Text(stringResource(R.string.transcribe))
                    }
                }
            }
        }
    }
}
