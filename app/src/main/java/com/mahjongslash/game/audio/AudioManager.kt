package com.mahjongslash.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.mahjongslash.R

/**
 * Manages all game audio via SoundPool for SFX.
 * All sounds are loaded at init time — no allocations during gameplay.
 */
class AudioManager(private val context: Context) {

    private val soundPool: SoundPool
    private val sounds = mutableMapOf<SfxType, Int>()
    private var enabled = true

    // Ambient music via MediaPlayer
    private var musicPlayer: MediaPlayer? = null
    private var currentMusicRes: Int = 0

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attrs)
            .build()

        // Load all SFX
        sounds[SfxType.SLASH_VALID] = soundPool.load(context, R.raw.slash_valid, 1)
        sounds[SfxType.SLASH_INVALID] = soundPool.load(context, R.raw.slash_invalid, 1)
        sounds[SfxType.TILE_SHATTER] = soundPool.load(context, R.raw.tile_shatter, 1)
        sounds[SfxType.COMBO_INCREMENT] = soundPool.load(context, R.raw.combo_increment, 1)
        sounds[SfxType.COMBO_BREAK] = soundPool.load(context, R.raw.combo_break, 1)
        sounds[SfxType.BLADE_CRACK] = soundPool.load(context, R.raw.blade_crack, 1)
        sounds[SfxType.GAME_OVER] = soundPool.load(context, R.raw.game_over, 1)
        sounds[SfxType.POWER_UP] = soundPool.load(context, R.raw.power_up, 1)
        sounds[SfxType.MENU_TAP] = soundPool.load(context, R.raw.menu_tap, 1)
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    /**
     * Play a sound effect. Rate adjusts pitch (1.0 = normal, higher = faster/higher pitch).
     */
    fun play(sfx: SfxType, rate: Float = 1.0f, volume: Float = 1.0f) {
        if (!enabled) return
        val soundId = sounds[sfx] ?: return
        soundPool.play(soundId, volume, volume, 1, 0, rate.coerceIn(0.5f, 2.0f))
    }

    /**
     * Play slash valid with pitch scaled by combo level.
     */
    fun playSlashValid(comboLevel: Int) {
        val rate = 1.0f + (comboLevel - 1) * 0.1f
        play(SfxType.SLASH_VALID, rate = rate.coerceAtMost(1.5f))
        play(SfxType.TILE_SHATTER)
    }

    /**
     * Start looping background music. Pass a raw resource ID (e.g. R.raw.music_menu).
     * If the same track is already playing, does nothing.
     */
    fun startMusic(resId: Int, volume: Float = 0.4f) {
        if (!enabled) return
        if (currentMusicRes == resId && musicPlayer?.isPlaying == true) return
        stopMusic()
        try {
            musicPlayer = MediaPlayer.create(context, resId)?.apply {
                isLooping = true
                setVolume(volume, volume)
                start()
            }
            currentMusicRes = resId
        } catch (_: Exception) {
            // Music file may not exist yet — fail silently
        }
    }

    fun stopMusic() {
        musicPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        musicPlayer = null
        currentMusicRes = 0
    }

    fun release() {
        stopMusic()
        soundPool.release()
    }
}

enum class SfxType {
    SLASH_VALID,
    SLASH_INVALID,
    TILE_SHATTER,
    COMBO_INCREMENT,
    COMBO_BREAK,
    BLADE_CRACK,
    GAME_OVER,
    POWER_UP,
    MENU_TAP,
}
