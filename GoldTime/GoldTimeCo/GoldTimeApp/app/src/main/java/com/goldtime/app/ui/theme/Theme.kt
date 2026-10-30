package com.goldtime.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** Exact palette taken from the Gold Time Co. design. */
object GoldColors {
    val Background = Color(0xFF070707)
    val Surface = Color(0xFF161616)
    val Field = Color(0xFF1B1B1B)
    val FieldBorder = Color(0xFF262626)
    val Gold = Color(0xFFD4B460)
    val GoldDark = Color(0xFFA98A32)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextMuted = Color(0xFF8A8A8A)
    val Divider = Color(0xFF232323)
    val Error = Color(0xFFE57373)
}

val GoldGradient = Brush.horizontalGradient(listOf(Color(0xFFD4B460), Color(0xFFA98A32)))

/**
 * Headings use a serif face (the design uses Playfair Display).
 * To use Playfair exactly: add playfair_display.ttf to res/font and set
 *   val HeadingFont = FontFamily(Font(R.font.playfair_display))
 */
val HeadingFont: FontFamily = FontFamily.Serif

@Composable
fun GoldTimeTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = GoldColors.Gold,
        onPrimary = Color.Black,
        secondary = GoldColors.GoldDark,
        background = GoldColors.Background,
        onBackground = GoldColors.TextPrimary,
        surface = GoldColors.Surface,
        onSurface = GoldColors.TextPrimary,
        surfaceVariant = GoldColors.Field,
        onSurfaceVariant = GoldColors.TextMuted,
        outline = GoldColors.FieldBorder,
        error = GoldColors.Error
    )
    MaterialTheme(colorScheme = colors, content = content)
}
