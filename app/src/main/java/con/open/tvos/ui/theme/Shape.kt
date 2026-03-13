package con.open.tvos.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * TVBox Shapes for Android TV
 * Rounded corners optimized for TV screen aesthetics
 */

val TVBoxShapes = Shapes(
    // Extra Small - Chips, small badges
    extraSmall = RoundedCornerShape(4.dp),

    // Small - Buttons, text fields
    small = RoundedCornerShape(8.dp),

    // Medium - Cards, dialogs
    medium = RoundedCornerShape(12.dp),

    // Large - Large cards, bottom sheets
    large = RoundedCornerShape(16.dp),

    // Extra Large - Full-screen dialogs, major UI elements
    extraLarge = RoundedCornerShape(24.dp)
)

/**
 * Card shapes for different contexts
 */
object CardShapes {
    // Poster cards (movie posters)
    val Poster = RoundedCornerShape(8.dp)

    // Episode cards (wider aspect ratio)
    val Episode = RoundedCornerShape(8.dp)

    // Live TV channel cards
    val Channel = RoundedCornerShape(12.dp)

    // Settings cards
    val Settings = RoundedCornerShape(12.dp)

    // Category/category selection cards
    val Category = RoundedCornerShape(16.dp)
}

/**
 * Focus indicator shapes
 */
object FocusShapes {
    // Focus ring for cards
    val FocusRing = RoundedCornerShape(12.dp)

    // Focus indicator for buttons
    val ButtonFocus = RoundedCornerShape(8.dp)
}
