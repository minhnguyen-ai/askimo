/* SPDX-License-Identifier: AGPLv3
 *
 * Copyright (c) 2026 Askimo
 */
package io.askimo.ui.voice

import io.askimo.core.logging.logger
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.UnsupportedAudioFileException

/** Thrown when audio playback cannot start (unsupported format, no output device, etc.). */
class AudioPlaybackException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Plays synthesised audio using [javax.sound.sampled] — no extra native dependencies needed.
 * Supports [pause]/[resume] and [stop]. Handles MP3 (OpenAI TTS) transparently: the JDK's
 * built-in `javax.sound.sampled` SPI only decodes WAV/AIFF/AU natively, so MP3 bytes are
 * transcoded to a PCM-decodable stream via [AudioSystem.getAudioInputStream] chaining, which
 * works out of the box as long as an MP3 SPI provider is on the classpath (see call site notes).
 *
 * [AudioPlayer.Companion.stopCurrent] enforces the "only one message plays at a time" rule from
 * the voice-output UX spec — callers (e.g. the 🔊 button in `MessageComponents.kt`) should call
 * [play] via the companion so any previously-playing clip is stopped first.
 */
class AudioPlayer {
    private val log = logger<AudioPlayer>()

    @Volatile private var clip: Clip? = null
    @Volatile private var onFinished: (() -> Unit)? = null

    /** True while a clip is actively playing (not paused, not stopped). */
    val isPlaying: Boolean get() = clip?.isRunning == true

    /**
     * Decodes [audio] (format described by [format]) and starts playback.
     * @param onComplete Invoked on natural playback completion (not on [stop]).
     * @throws AudioPlaybackException if the audio cannot be decoded or no output device is available.
     */
    fun play(audio: ByteArray, format: VoiceAudioFormat, onComplete: () -> Unit = {}) {
        stop()

        try {
            val rawStream = AudioSystem.getAudioInputStream(ByteArrayInputStream(audio))
            val decodedFormat = AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                rawStream.format.sampleRate,
                16,
                rawStream.format.channels,
                rawStream.format.channels * 2,
                rawStream.format.sampleRate,
                false,
            )
            val decodedStream = AudioSystem.getAudioInputStream(decodedFormat, rawStream)

            val newClip = AudioSystem.getClip()
            newClip.open(decodedStream)
            newClip.addLineListener { event ->
                if (event.type == LineEvent.Type.STOP && newClip.framePosition >= newClip.frameLength) {
                    onFinished?.invoke()
                }
            }
            onFinished = onComplete
            clip = newClip
            newClip.start()
        } catch (e: UnsupportedAudioFileException) {
            throw AudioPlaybackException(
                "Unsupported audio format for playback (${format.name}). " +
                    "MP3 playback requires an MP3 SPI provider on the classpath.",
                e,
            )
        } catch (e: LineUnavailableException) {
            throw AudioPlaybackException("No audio output device available for playback.", e)
        } catch (e: Exception) {
            log.warn("Audio playback failed: {}", e.message)
            throw AudioPlaybackException("Could not play audio: ${e.message}", e)
        }
    }

    /** Pauses playback in place; [resume] continues from the same position. No-op if not playing. */
    fun pause() {
        clip?.takeIf { it.isRunning }?.stop()
    }

    /** Resumes playback from where it was paused. No-op if not paused or already playing. */
    fun resume() {
        clip?.takeIf { it.isOpen && !it.isRunning }?.start()
    }

    /** Stops playback and releases the underlying audio line. Safe to call when not playing. */
    fun stop() {
        clip?.let {
            if (it.isRunning) it.stop()
            it.close()
        }
        clip = null
        onFinished = null
    }

    companion object {
        @Volatile private var currentlyPlaying: AudioPlayer? = null

        /**
         * Ensures only one [AudioPlayer] instance plays at a time across the whole app —
         * stops any previously-registered player before returning, per the
         * "only one message plays at a time" requirement for message 🔊 buttons.
         */
        @Synchronized
        fun stopCurrent() {
            currentlyPlaying?.stop()
            currentlyPlaying = null
        }

        /** Registers [player] as the currently-playing instance, stopping any previous one first. */
        @Synchronized
        fun register(player: AudioPlayer) {
            if (currentlyPlaying !== player) {
                currentlyPlaying?.stop()
            }
            currentlyPlaying = player
        }
    }
}

