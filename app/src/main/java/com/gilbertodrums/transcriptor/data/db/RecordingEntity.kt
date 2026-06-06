package com.gilbertodrums.transcriptor.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gilbertodrums.transcriptor.data.llm.Summary
import java.time.LocalDateTime

/**
 * Fila de Room que persiste una grabación. Mapea 1:1 con el modelo de dominio
 * [com.gilbertodrums.transcriptor.domain.model.Recording].
 *
 * `createdAt` y `summary` se guardan vía los [Converters] registrados en la base de datos.
 */
@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: LocalDateTime,
    val filePath: String,
    val transcript: String?,
    val summary: Summary?
)
