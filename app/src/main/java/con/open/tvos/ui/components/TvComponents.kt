package con.open.tvos.ui.components

import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import con.open.tvos.ui.theme.Background
import con.open.tvos.ui.theme.CardBackground
import con.open.tvos.ui.theme.CardFocused
import con.open.tvos.ui.theme.FocusOuter
import con.open.tvos.ui.theme.GradientEnd
import con.open.tvos.ui.theme.GradientStart
import con.open.tvos.ui.theme.Primary
import con.open.tvos.ui.theme.Surface
import con.open.tvos.ui.theme.TextPrimary
import con.open.tvos.ui.theme.TextSecondary

/**
 * TV Button Component
 * 
 * A button optimized for TV D-pad navigation with focus indication.
 * 
 * @param text The button text
 * @param onClick Click callback
 * @param modifier Modifier
 * @param enabled Whether the button is enabled
 * @param isFocused Whether the button is currently focused (optional, auto-managed if null)
 * @param leadingIcon Optional leading icon composable
 * @param trailingIcon Optional trailing icon composable
 */
@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isFocused: Boolean? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val actualFocused = isFocused ?: focused

    val backgroundColor by animateColorAsState(
        targetValue = if (actualFocused) Primary else CardBackground,
        label = "backgroundColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (actualFocused) Color.White else TextPrimary,
        label = "textColor"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (actualFocused) 3.dp else 0.dp,
        label = "borderWidth"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(
                if (actualFocused) {
                    Modifier.border(borderWidth, FocusOuter, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .tvFocusable(
                enabled = enabled,
                interactionSource = interactionSource
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingIcon?.invoke()
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        trailingIcon?.invoke()
    }
}

/**
 * TV Card Component
 * 
 * A card component optimized for TV D-pad navigation.
 * Commonly used for movie/poster cards in grids.
 * 
 * @param onClick Click callback
 * @param modifier Modifier
 * @param isFocused Whether the card is currently focused (optional, auto-managed if null)
 * @param showGlow Whether to show glow effect on focus
 * @param showScale Whether to scale on focus
 * @param content Card content
 */
@Composable
fun TvCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFocused: Boolean? = null,
    showGlow: Boolean = true,
    showScale: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val actualFocused = isFocused ?: focused

    val backgroundColor by animateColorAsState(
        targetValue = if (actualFocused) CardFocused else CardBackground,
        label = "backgroundColor"
    )

    Box(
        modifier = modifier
            .then(if (showScale) Modifier.tvFocusScale(actualFocused) else Modifier)
            .then(if (showGlow) Modifier.tvFocusGlow(actualFocused) else Modifier)
            .tvFocusBorder(actualFocused)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .tvFocusable(interactionSource = interactionSource),
        content = content
    )
}

/**
 * TV Poster Card Component
 * 
 * A card specifically designed for movie/poster thumbnails.
 * Includes image, title overlay, and optional subtitle.
 * 
 * @param title The movie/show title
 * @param modifier Modifier
 * @param subtitle Optional subtitle (e.g., episode count, rating)
 * @param imageUrl Image URL (placeholder - actual image loading needs implementation)
 * @param onClick Click callback
 * @param isFocused Whether the card is focused (optional, auto-managed if null)
 */
@Composable
fun TvPosterCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    imageUrl: String? = null,
    onClick: () -> Unit,
    isFocused: Boolean? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val actualFocused = isFocused ?: focused

    TvFocusableContainer(
        isFocused = actualFocused,
        modifier = modifier
            .width(150.dp)
            .height(220.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .tvFocusable(interactionSource = interactionSource),
        showGlow = true,
        showScale = true
    ) {
        // Image placeholder (actual image loading requires Coil/Glide integration)
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Surface)
        ) {
            // TODO: Add actual image loading with Coil
            // AsyncImage(
            //     model = imageUrl,
            //     contentDescription = title,
            //     contentScale = ContentScale.Crop
            // )
        }

        // Gradient overlay for text
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(80.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(GradientStart, GradientEnd)
                    )
                )
        )

        // Title and subtitle
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * TV Wide Card Component
 * 
 * A wide card for episode selection, live channels, etc.
 * Features image on left, content on right.
 * 
 * @param title The title
 * @param modifier Modifier
 * @param subtitle Optional subtitle
 * @param imageUrl Optional image URL
 * @param onClick Click callback
 * @param isFocused Whether the card is focused (optional, auto-managed if null)
 * @param trailingContent Optional trailing content (e.g., duration, badges)
 */
@Composable
fun TvWideCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    imageUrl: String? = null,
    onClick: () -> Unit,
    isFocused: Boolean? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val actualFocused = isFocused ?: focused

    val backgroundColor by animateColorAsState(
        targetValue = if (actualFocused) CardFocused else CardBackground,
        label = "backgroundColor"
    )

    Row(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(if (actualFocused) Modifier.tvFocusScale(actualFocused, 1.02f) else Modifier)
            .tvFocusBorder(actualFocused)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .tvFocusable(interactionSource = interactionSource)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Image placeholder
        Box(
            modifier = Modifier
                .size(width = 160.dp, height = 104.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Surface)
        ) {
            // TODO: Add actual image loading
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .height(104.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Trailing content
        trailingContent?.let {
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = it
            )
        }
    }
}

/**
 * TV Category Header Component
 * 
 * A header for category rows in the home screen.
 * 
 * @param title The category title
 * @param modifier Modifier
 * @param onMoreClick Optional callback for "See More" button
 */
@Composable
fun TvCategoryHeader(
    title: String,
    modifier: Modifier = Modifier,
    onMoreClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TextPrimary,
            style = MaterialTheme.typography.headlineSmall
        )
        onMoreClick?.let {
            TvButton(
                text = "查看更多",
                onClick = it,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

/**
 * TV Badge Component
 * 
 * A small badge for displaying status, ratings, etc.
 * 
 * @param text Badge text
 * @param modifier Modifier
 * @param backgroundColor Background color
 * @param textColor Text color
 */
@Composable
fun TvBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Primary,
    textColor: Color = Color.White
) {
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = textColor,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1
    )
}

/**
 * TV Live Badge Component
 * 
 * A special badge for live TV channels.
 */
@Composable
fun TvLiveBadge(
    modifier: Modifier = Modifier
) {
    TvBadge(
        text = "LIVE",
        modifier = modifier,
        backgroundColor = Color(0xFFE53935),
        textColor = Color.White
    )
}

/**
 * TV Rating Badge Component
 * 
 * A badge for displaying ratings.
 * 
 * @param rating The rating value (0-10)
 */
@Composable
fun TvRatingBadge(
    rating: Float,
    modifier: Modifier = Modifier
) {
    val color = when {
        rating >= 8.0f -> Color(0xFF4CAF50)
        rating >= 6.0f -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    
    TvBadge(
        text = String.format("%.1f", rating),
        modifier = modifier,
        backgroundColor = color,
        textColor = Color.White
    )
}

/**
 * TV Section Title Component
 * 
 * A large section title for detail pages.
 */
@Composable
fun TvSectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier.padding(vertical = 16.dp),
        color = TextPrimary,
        style = MaterialTheme.typography.headlineMedium
    )
}
