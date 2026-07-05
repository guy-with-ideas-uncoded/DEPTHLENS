package com.example.communication

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CommunicationStateManager {
    private const val TAG = "CommunicationStateMgr"
    private val _state = MutableStateFlow(CommunicationState.Idle)
    val state: StateFlow<CommunicationState> = _state.asStateFlow()

    @Synchronized
    fun transitionTo(newState: CommunicationState) {
        if (_state.value == newState) return
        Log.i(TAG, "State Transition: ${_state.value} -> $newState")
        _state.value = newState
    }

    @Synchronized
    fun getCurrentState(): CommunicationState = _state.value

    @Synchronized
    fun reset() {
        transitionTo(CommunicationState.Idle)
    }
}
