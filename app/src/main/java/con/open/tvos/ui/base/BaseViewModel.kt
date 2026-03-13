package con.open.tvos.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel for all screens
 * 
 * Provides:
 * - State management with StateFlow
 * - Event handling with SharedFlow
 * - Effect handling with Channel (for one-time events)
 * 
 * @param S The UI State type
 * @param E The UI Event type
 * @param F The UI Effect type
 */
abstract class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    initialState: S
) : ViewModel() {

    // UI State
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    // UI Events (can be consumed by multiple observers)
    private val _events = MutableSharedFlow<E>()
    val events: SharedFlow<E> = _events.asSharedFlow()

    // UI Effects (one-time events like navigation, toasts)
    private val _effects = Channel<F>()
    val effects: Flow<F> = _effects.receiveAsFlow()

    /**
     * Update the current state
     */
    protected fun updateState(reducer: S.() -> S) {
        val currentState = _state.value
        _state.value = currentState.reducer()
    }

    /**
     * Get current state value
     */
    protected val currentState: S
        get() = _state.value

    /**
     * Send an event
     */
    protected fun sendEvent(event: E) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }

    /**
     * Send an effect (one-time event)
     */
    protected fun sendEffect(effect: F) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }

    /**
     * Handle an event - override in subclasses
     */
    open fun onEvent(event: E) {
        // Override in subclasses
    }

    /**
     * Set loading state
     */
    protected fun setLoading(loading: Boolean) {
        updateState { 
            @Suppress("UNCHECKED_CAST")
            copy(isLoading = loading) as S
        }
    }

    /**
     * Set error state
     */
    protected fun setError(error: String?) {
        updateState {
            @Suppress("UNCHECKED_CAST")
            copy(error = error) as S
        }
    }

    /**
     * Clear error
     */
    protected fun clearError() {
        updateState {
            @Suppress("UNCHECKED_CAST")
            copy(error = null) as S
        }
    }
}
