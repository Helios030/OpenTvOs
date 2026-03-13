package con.open.tvos.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Spring.StiffnessLow
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import con.open.tvos.ui.theme.FocusOuter
import con.open.tvos.ui.theme.Primary

/**
 * TV Focus State holder
 * Tracks focus state for TV D-pad navigation
 */
data class TvFocusState(
    val isFocused: Boolean = false,
    val hasFocus: Boolean = false
)

/**
 * Default animation spec for TV focus changes
 */
val TvFocusAnimationSpec: AnimationSpec<Dp> = spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = StiffnessLow
)

/**
 * TV Focusable Modifier
 * 
 * Provides focus indication with animated border for TV D-pad navigation.
 * This is the core modifier for all TV-focused components.
 *
 * Usage:
 * ```kotlin
 * Box(
 *     modifier = Modifier.tvFocusable(
 *         onFocus = { /* handle focus */ },
 *         onClick = { /* handle click */ }
 *     )
 * ) {
 *     // content
 * }
 * ```
 *
 * @param onFocus Callback when focus state changes
 * @param onClick Callback when item is clicked/selected
 * @param enabled Whether the item can be focused
 * @param focusRequester Optional FocusRequester for programmatic focus control
 * @param interactionSource Optional InteractionSource for custom interactions
 */
fun Modifier.tvFocusable(
    onFocus: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed {
    val actualInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isFocused by actualInteractionSource.collectIsFocusedAsState()
    
    var currentOnFocus by remember { mutableStateOf(onFocus) }
    
    LaunchedEffect(isFocused) {
        currentOnFocus?.invoke(isFocused)
    }
    
    var modifier = this
        .focusable(interactionSource = actualInteractionSource)
        .onFocusChanged { focusState ->
            if (focusState.isFocused) {
                onFocus?.invoke(true)
            }
        }
    
    if (focusRequester != null) {
        modifier = modifier.focusRequester(focusRequester)
    }
    
    modifier
}

/**
 * TV Focus Border Modifier
 * 
 * Adds an animated focus border that appears when the component is focused.
 * The border grows from inside the component with a smooth animation.
 *
 * @param isFocused Whether the component is currently focused
 * @param borderColor The color of the focus border
 * @param borderWidth The width of the focus border when focused
 * @param cornerRadius The corner radius of the border
 * @param animationSpec The animation spec for border changes
 */
fun Modifier.tvFocusBorder(
    isFocused: Boolean,
    borderColor: Color = FocusOuter,
    borderWidth: Dp = 3.dp,
    cornerRadius: Dp = 12.dp,
    animationSpec: AnimationSpec<Dp> = TvFocusAnimationSpec
): Modifier = composed {
    val animatedBorderWidth by animateDpAsState(
        targetValue = if (isFocused) borderWidth else 0.dp,
        animationSpec = animationSpec,
        label = "borderWidth"
    )
    
    val animatedPadding by animateDpAsState(
        targetValue = if (isFocused) borderWidth else 0.dp,
        animationSpec = animationSpec,
        label = "padding"
    )
    
    this
        .padding(animatedPadding)
        .drawBehind {
            if (animatedBorderWidth > 0.dp) {
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(cornerRadius.toPx()),
                    style = Stroke(width = animatedBorderWidth.toPx())
                )
            }
        }
}

/**
 * TV Focus Scale Modifier
 * 
 * Slightly scales the component when focused for visual feedback.
 * Commonly used with TV cards and buttons.
 *
 * @param isFocused Whether the component is currently focused
 * @param focusedScale The scale factor when focused (default 1.05)
 * @param animationSpec The animation spec for scale changes
 */
fun Modifier.tvFocusScale(
    isFocused: Boolean,
    focusedScale: Float = 1.05f,
    animationSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = StiffnessLow
    )
): Modifier = composed {
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1f,
        animationSpec = animationSpec,
        label = "scale"
    )
    
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * TV Focus Glow Modifier
 * 
 * Adds a subtle glow effect around the component when focused.
 * Creates a cinematic effect suitable for TV screens.
 *
 * @param isFocused Whether the component is currently focused
 * @param glowColor The color of the glow
 * @param glowRadius The radius of the glow effect
 */
fun Modifier.tvFocusGlow(
    isFocused: Boolean,
    glowColor: Color = FocusOuter.copy(alpha = 0.5f),
    glowRadius: Dp = 8.dp
): Modifier = composed {
    val animatedGlowRadius by animateDpAsState(
        targetValue = if (isFocused) glowRadius else 0.dp,
        animationSpec = TvFocusAnimationSpec,
        label = "glowRadius"
    )
    
    this.drawBehind {
        if (animatedGlowRadius > 0.dp) {
            drawRoundRect(
                color = glowColor,
                topLeft = Offset(-animatedGlowRadius.toPx(), -animatedGlowRadius.toPx()),
                size = Size(
                    size.width + animatedGlowRadius.toPx() * 2,
                    size.height + animatedGlowRadius.toPx() * 2
                ),
                cornerRadius = CornerRadius(12.dp.toPx() + animatedGlowRadius.toPx()),
                style = Stroke(width = animatedGlowRadius.toPx())
            )
        }
    }
}

/**
 * TV Focusable Card Container
 * 
 * A pre-configured Box with TV focus capabilities.
 * Combines border, scale, and glow effects for a complete TV focus experience.
 *
 * @param isFocused Whether the card is currently focused
 * @param modifier Additional modifier
 * @param borderColor The focus border color
 * @param showGlow Whether to show the glow effect
 * @param showScale Whether to apply scale effect
 * @param content The card content
 */
@Composable
fun TvFocusableContainer(
    isFocused: Boolean,
    modifier: Modifier = Modifier,
    borderColor: Color = FocusOuter,
    showGlow: Boolean = true,
    showScale: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .then(if (showScale) Modifier.tvFocusScale(isFocused) else Modifier)
            .then(if (showGlow) Modifier.tvFocusGlow(isFocused) else Modifier)
            .tvFocusBorder(
                isFocused = isFocused,
                borderColor = borderColor
            ),
        content = content
    )
}

/**
 * Remember Focus State
 * 
 * Creates and remembers a TvFocusState for managing focus state.
 *
 * @param initialFocused Initial focus state
 * @return TvFocusState instance
 */
@Composable
fun rememberTvFocusState(initialFocused: Boolean = false): TvFocusState {
    return remember { TvFocusState(isFocused = initialFocused) }
}

/**
 * TV Focus Requester Provider
 * 
 * Provides a FocusRequester for programmatic focus control.
 * Useful for initial focus handling and focus navigation.
 *
 * Usage:
 * ```kotlin
 * val focusRequester = rememberTvFocusRequester()
 * 
 * LaunchedEffect(Unit) {
 *     focusRequester.requestFocus()
 * }
 * ```
 */
@Composable
fun rememberTvFocusRequester(): FocusRequester {
    return remember { FocusRequester() }
}
