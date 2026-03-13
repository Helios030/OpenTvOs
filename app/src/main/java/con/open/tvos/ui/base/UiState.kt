package con.open.tvos.ui.base

/**
 * Base UI State interface
 * All screen states should implement this interface
 */
interface UiState {
    val isLoading: Boolean
    val error: String?
}

/**
 * Base UI Event interface
 * All screen events should implement this interface
 */
interface UiEvent

/**
 * Base UI Effect interface
 * One-time effects (navigation, toasts, etc.) should implement this interface
 */
interface UiEffect

/**
 * Loading State
 * Generic loading state for screens
 */
data class LoadingState(
    override val isLoading: Boolean = true,
    override val error: String? = null
) : UiState

/**
 * Error State
 * Generic error state for screens
 */
data class ErrorState(
    val message: String,
    override val isLoading: Boolean = false,
    override val error: String = message
) : UiState

/**
 * Success State
 * Generic success state wrapper
 */
data class SuccessState<T>(
    val data: T,
    override val isLoading: Boolean = false,
    override val error: String? = null
) : UiState
