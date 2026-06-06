package com.gilbertodrums.transcriptor.data

import android.content.Context
import com.gilbertodrums.transcriptor.data.db.AppDatabase
import com.gilbertodrums.transcriptor.data.db.toDomain
import com.gilbertodrums.transcriptor.data.db.toEntity
import com.gilbertodrums.transcriptor.domain.model.Recording
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Única puerta de acceso a la persistencia de grabaciones. Trabaja con el modelo de dominio
 * [Recording] y oculta Room al resto de la app (UI/ViewModel).
 */
class RecordingRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).recordingDao()

    /** Todas las grabaciones, la más reciente primero. Se actualiza solo al cambiar la BD. */
    val recordings: Flow<List<Recording>> =
        dao.getAllFlow().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: String): Recording? = dao.getById(id)?.toDomain()

    /** Inserta o actualiza (REPLACE): sirve para crear y para guardar transcripción/resumen. */
    suspend fun save(recording: Recording) = dao.insert(recording.toEntity())

    suspend fun delete(recording: Recording) = dao.delete(recording.toEntity())
}
