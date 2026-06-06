package com.gilbertodrums.transcriptor.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a la tabla `recordings`. Las operaciones de escritura son `suspend`
 * (se ejecutan fuera del hilo principal); la lectura es un [Flow] que emite
 * automáticamente cada vez que cambian los datos.
 */
@Dao
interface RecordingDao {

    /** Lista todas las grabaciones, la más reciente primero. */
    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getById(id: String): RecordingEntity?

    /** Inserta o reemplaza (sirve también para guardar cambios sobre una existente). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: RecordingEntity)

    @Update
    suspend fun update(recording: RecordingEntity)

    @Delete
    suspend fun delete(recording: RecordingEntity)
}
