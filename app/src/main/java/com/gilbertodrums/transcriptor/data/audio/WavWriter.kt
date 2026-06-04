package com.gilbertodrums.transcriptor.data.audio

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Escribe un archivo WAV (PCM 16-bit, mono, 16 kHz) de forma progresiva.
 * Usa un header placeholder de 44 bytes al inicio; al cerrar actualiza los tamaños reales.
 */
internal class WavWriter(
    file: File,
    private val sampleRate: Int = 16_000,
    private val channels: Int = 1,
    private val bitsPerSample: Int = 16
) {
    private val raf = RandomAccessFile(file, "rw")
    private var pcmBytes = 0L

    init {
        raf.write(ByteArray(44)) // placeholder; reemplazado en close()
    }

    fun writeShorts(buffer: ShortArray, count: Int) {
        val bytes = ByteBuffer.allocate(count * 2)
            .order(ByteOrder.LITTLE_ENDIAN)
            .also { buf -> repeat(count) { buf.putShort(buffer[it]) } }
            .array()
        raf.write(bytes)
        pcmBytes += bytes.size
    }

    fun close() {
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val blockAlign = channels * (bitsPerSample / 8)

        raf.seek(0)
        fun intLE(v: Int) { raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array()) }
        fun shortLE(v: Short) { raf.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(v).array()) }

        raf.write("RIFF".toByteArray())
        intLE((36 + pcmBytes).toInt())      // ChunkSize
        raf.write("WAVE".toByteArray())
        raf.write("fmt ".toByteArray())
        intLE(16)                            // Subchunk1Size
        shortLE(1)                           // AudioFormat: PCM
        shortLE(channels.toShort())
        intLE(sampleRate)
        intLE(byteRate)
        shortLE(blockAlign.toShort())
        shortLE(bitsPerSample.toShort())
        raf.write("data".toByteArray())
        intLE(pcmBytes.toInt())              // Subchunk2Size

        raf.close()
    }
}
