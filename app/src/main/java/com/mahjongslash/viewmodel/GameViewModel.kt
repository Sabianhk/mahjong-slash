package com.mahjongslash.viewmodel

import android.app.Application
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import com.mahjongslash.game.audio.AudioManager
import com.mahjongslash.game.audio.SfxType
import com.mahjongslash.game.engine.GameEngine
import com.mahjongslash.game.engine.GameEventListener
import com.mahjongslash.game.engine.GameState
import com.mahjongslash.game.render.BackgroundRenderer
import com.mahjongslash.game.render.TileBitmapCache
import com.mahjongslash.game.render.TileRenderer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = GameEngine()
    private val audioManager = AudioManager(application)
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var initialized = false
    private var bitmapCache: TileBitmapCache? = null
    private var _backgroundRenderer: BackgroundRenderer? = null
    val bgRenderer: BackgroundRenderer? get() = _backgroundRenderer

    /** Set from the Composable to enable haptic feedback */
    var hapticView: View? = null

    init {
        engine.eventListener = object : GameEventListener {
            override fun onSlashValid(comboLevel: Int) {
                audioManager.playSlashValid(comboLevel)
                haptic(HapticFeedbackConstants.CONTEXT_CLICK)
            }
            override fun onSlashInvalid() {
                audioManager.play(SfxType.SLASH_INVALID)
                haptic(HapticFeedbackConstants.LONG_PRESS)
            }
            override fun onComboIncrement(comboLevel: Int) {
                audioManager.play(SfxType.COMBO_INCREMENT, rate = 1.0f + (comboLevel - 2) * 0.08f)
            }
            override fun onComboBreak() {
                audioManager.play(SfxType.COMBO_BREAK)
            }
            override fun onBladeDamage(remainingHealth: Int) {
                audioManager.play(SfxType.BLADE_CRACK)
                haptic(HapticFeedbackConstants.LONG_PRESS)
            }
            override fun onGameOver() {
                audioManager.play(SfxType.GAME_OVER)
                haptic(HapticFeedbackConstants.LONG_PRESS)
            }
        }
    }

    fun initializeIfNeeded(width: Float, height: Float, density: Float) {
        if (!initialized || _state.value.screenWidth != width || _state.value.screenHeight != height) {
            if (bitmapCache == null) {
                bitmapCache = TileBitmapCache(density).also {
                    it.initialize()
                    TileRenderer.bitmapCache = it
                }
            }
            if (_backgroundRenderer == null) {
                _backgroundRenderer = BackgroundRenderer(density)
            }
            _backgroundRenderer?.initialize(width.toInt(), height.toInt())

            engine.initialize(width, height, density)
            initialized = true
        }
    }

    fun update(dt: Float) {
        _state.value = engine.update(dt)
    }

    fun pause() = engine.pause()
    fun resume() = engine.resume()

    fun onSlashStart(position: Offset) = engine.onSlashStart(position)
    fun onSlashMove(position: Offset) = engine.onSlashMove(position)
    fun onSlashEnd(position: Offset) = engine.onSlashEnd(position)
    fun onSlashEndAtLastPosition() = engine.onSlashEndAtLastPosition()

    fun restart(width: Float, height: Float, density: Float) {
        engine.initialize(width, height, density)
        initialized = true
    }

    private fun haptic(feedbackConstant: Int) {
        hapticView?.performHapticFeedback(feedbackConstant)
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }
}
