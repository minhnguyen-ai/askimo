/* SPDX-License-Identifier: AGPLv3
 *
 * Copyright (c) 2026 Askimo
 */
package io.askimo.ui.voice

import io.askimo.core.logging.logger
import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.TargetDataLine
import kotlin.math.sqrt

/**
 * Thrown when the microphone cannot be opened — typically because the OS denied audio
 * permission (macOS TCC prompt declined/never granted) or no input device is available.
 * Callers should catch this and surface a user-facing message (e.g. via `AppErrorEvent`)
 * rather than a raw stack trace.
 */
class MicrophoneUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Captures microphone audio using [javax.sound.sampled] — no extra native dependencies needed.
 * Records 16kHz mono 16-bit PCM, which is natively accepted by Whisper-family STT APIs.
 *
 * Usage:
 * ```
 * val recorder = AudioRecorder()
 * recorder.start()          // begins capturing on a background thread
 * ...
 * val wavBytes = recorder.stop()   // returns a complete WAV file's bytes
 * // or recorder.cancel() to discard without producing output
 * ```
 *
 * Not thread-safe for concurrent start/stop calls from multiple threads — intended to be driven
 * from a single UI-owned coroutine/state machine (see the 🎤 button in `ChatInputField.kt`).
 */
class AudioRecorder {
    private val log = logger<AudioRecorder>()

    companion object {
        private const val SAMPLE_RATE = 16_000f
        private const val SAMPLE_SIZE_BITS = 16
        private const val CHANNELS = 1
        private const val SIGNED = true
        private const val BIG_ENDIAN = false
        private const val READ_BUFFER_SIZE = 4096
    }

    private val format = AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_BITS, CHANNELS, SIGNED, BIG_ENDIAN)

    @Volatile private var line: TargetDataLine? = null
    private var captureThread: Thread? = null
    private val buffer = ByteArrayOutputStream()

    /** True while actively capturing audio. */
    val isRecording: Boolean get() = line?.isOpen == true

    /**
     * Opens the microphone and begins capturing on a background thread.
     *
     * @param onLevel Optional callback invoked on the capture thread after each chunk is read,
     *   with the chunk's normalized RMS amplitude in `0f..1f` (0 = silence, 1 = full-scale).
     *   Intended to drive a live "recording" UI indicator (e.g. a waveform/level meter) — this
     *   is a lightweight, high-frequency signal, not audio data, so callers can safely mutate
     *   Compose state directly from it (see the 🎤 button in `ChatInputField.kt`).
     * @throws MicrophoneUnavailableException if no input device is available or the OS denies access.
     */
    fun start(onLevel: ((Float) -> Unit)? = null) {
        check(!isRecording) { "AudioRecorder is already recording" }
        buffer.reset()

        val info = DataLine.Info(TargetDataLine::class.java, format)
        if (!AudioSystem.isLineSupported(info)) {
            throw MicrophoneUnavailableException("No microphone input line supports the required audio format")
        }

        val targetLine = try {
            (AudioSystem.getLine(info) as TargetDataLine).also {
                it.open(format)
                it.start()
            }
        } catch (e: LineUnavailableException) {
            throw MicrophoneUnavailableException(
                "Could not open the microphone. Check system permissions for Askimo under " +
                    "Privacy & Security > Microphone. (${e.message})",
                e,
            )
        } catch (e: SecurityException) {
            throw MicrophoneUnavailableException("Microphone access was denied by the operating system.", e)
        }

        line = targetLine
        captureThread = Thread({
            val readBuffer = ByteArray(READ_BUFFER_SIZE)
            try {
                while (targetLine.isOpen) {
                    val bytesRead = targetLine.read(readBuffer, 0, readBuffer.size)
                    if (bytesRead > 0) {
                        synchronized(buffer) { buffer.write(readBuffer, 0, bytesRead) }
                        if (onLevel != null) {
                            onLevel(computeRmsLevel(readBuffer, bytesRead))
                        }
                    }
                }
            } catch (e: Exception) {
                log.debug("Audio capture thread stopped: {}", e.message)
            }
        }, "askimo-audio-recorder").apply {
            isDaemon = true
            start()
        }
    }

    /**
     * Stops recording and returns the captured audio encoded as a complete WAV file.
     * Safe to call even if [start] was never called (returns an empty WAV).
     */
    fun stop(): ByteArray {
        val targetLine = line ?: return encodeAsWav(ByteArray(0))
        targetLine.stop()
        targetLine.close()
        line = null
        captureThread?.join(2_000)
        captureThread = null

        val pcmBytes = synchronized(buffer) { buffer.toByteArray() }
        return encodeAsWav(pcmBytes)
    }

    /** Stops recording and discards the captured audio without producing output. */
    fun cancel() {
        line?.let {
            it.stop()
            it.close()
        }
        line = null
        captureThread?.join(2_000)
        captureThread = null
        synchronized(buffer) { buffer.reset() }
    }

    private fun encodeAsWav(pcmBytes: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        pcmBytes.inputStream().use { pcmStream ->
            val frameLength = pcmBytes.size / format.frameSize
            AudioInputStream(pcmStream, format, frameLength.toLong()).use { audioStream ->
                AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, output)
            }
        }
        return output.toByteArray()
    }

    /**
     * Computes the normalized RMS (root-mean-square) amplitude of a chunk of 16-bit
     * little-endian PCM samples, as a value in `0f..1f`.
     */
    private fun computeRmsLevel(buf: ByteArray, length: Int): Float {
        var sumSquares = 0.0
        var sampleCount = 0
        var i = 0
        while (i + 1 < length) {
            val sample = ((buf[i + 1].toInt() shl 8) or (buf[i].toInt() and 0xFF)).toShort()
            sumSquares += sample * sample.toDouble()
            sampleCount++
            i += 2
        }
        if (sampleCount == 0) return 0f
        val rms = sqrt(sumSquares / sampleCount)
        return (rms / Short.MAX_VALUE).toFloat().coerceIn(0f, 1f)
    }
}
