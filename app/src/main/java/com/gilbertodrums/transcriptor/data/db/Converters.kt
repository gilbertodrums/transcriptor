package com.gilbertodrums.transcriptor.data.db

import androidx.room.TypeConverter
import com.gilbertodrums.transcriptor.data.llm.Summary
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Conversores de tipos para Room. Usan `org.json` (incluido en Android, sin dependencias extra).
 *
 * - [LocalDateTime] ⇄ epoch millis (zona del sistema): permite ordenar por fecha en SQL.
 * - [Summary] ⇄ JSON: serializa los puntos y el flag IA/básico en una sola columna.
 */
class Converters {

    @TypeConverter
    fun localDateTimeToEpoch(value: LocalDateTime?): Long? =
        value?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

    @TypeConverter
    fun epochToLocalDateTime(value: Long?): LocalDateTime? =
        value?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) }

    @TypeConverter
    fun summaryToJson(value: Summary?): String? {
        if (value == null) return null
        val points = JSONArray().apply { value.points.forEach { put(it) } }
        return JSONObject()
            .put("points", points)
            .put("isAi", value.isAi)
            .toString()
    }

    @TypeConverter
    fun jsonToSummary(value: String?): Summary? {
        if (value == null) return null
        val obj = JSONObject(value)
        val arr = obj.getJSONArray("points")
        val points = ArrayList<String>(arr.length()).apply {
            for (i in 0 until arr.length()) add(arr.getString(i))
        }
        return Summary(points = points, isAi = obj.getBoolean("isAi"))
    }
}
