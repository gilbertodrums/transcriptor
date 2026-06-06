package com.gilbertodrums.transcriptor.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Base de datos Room de la app. Versión 1.
 *
 * `exportSchema = false` y migración destructiva: aceptable **solo** mientras la app no esté
 * publicada (ver tarea 4.8). Antes de publicar habrá que exportar el esquema y escribir
 * migraciones reales para no perder datos de usuarios en actualizaciones.
 */
@Database(entities = [RecordingEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recordingDao(): RecordingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "transcriptor.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
