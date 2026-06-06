package com.gilbertodrums.transcriptor.data.db

import com.gilbertodrums.transcriptor.domain.model.Recording

/**
 * Conversión entre la fila de Room ([RecordingEntity]) y el modelo de dominio ([Recording]).
 * Mantiene a `domain/` sin conocer Room: la app trabaja siempre con `Recording`.
 */
fun RecordingEntity.toDomain(): Recording = Recording(
    id = id,
    title = title,
    createdAt = createdAt,
    filePath = filePath,
    transcript = transcript,
    summary = summary
)

fun Recording.toEntity(): RecordingEntity = RecordingEntity(
    id = id,
    title = title,
    createdAt = createdAt,
    filePath = filePath,
    transcript = transcript,
    summary = summary
)
