package com.mahjongslash.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.mahjongslash.game.engine.GameEngine
import com.mahjongslash.game.engine.GameState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel : ViewModel() {

    private val engine = GameEngine()
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var initialized = false

    fun initializeIfNeeded(width: Float, height: Float, density: Float) {
        if (!initialized || _state.value.screenWidth != width || _state.value.screenHeight != height) {
            engine.initialize(width, height, density)
            initialized = true
        }
    }

    fun update(dt: Float) {
        _state.value = engine.update(dt)
    }

    fun onSlashStart(position: Offset) = engine.onSlashStart(position)
    fun onSlashMove(position: Offset) = engine.onSlashMove(position)
    fun onSlashEnd(position: Offset) = engine.onSlashEnd(position)
    fun onSlashEndAtLastPosition() = engine.onSlashEndAtLastPosition()

    fun restart(width: Float, height: Float, density: Float) {
        engine.initialize(width, height, density)
        initialized = true
    }

    fun triggerAutoSlash(): String = engine.triggerAutoSlash()
}
