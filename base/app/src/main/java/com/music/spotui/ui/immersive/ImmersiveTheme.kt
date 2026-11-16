package com.music.spotui.ui.immersive

import com.music.spotui.ui.theme.AppSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.spotui.ui.look.ImmersiveDisplay
import com.music.spotui.ui.look.ImmersiveSans

object Immersive {
    var scheme by androidx.compose.runtime.mutableStateOf(androidx.compose.material3.darkColorScheme())
    var systemDark by androidx.compose.runtime.mutableStateOf(true)
    val dark: Boolean get() = systemDark
    val Page: Color get() = scheme.background
    val Raised: Color get() = scheme.surfaceContainerHigh
    val Separator: Color get() = scheme.outlineVariant
    val Text: Color get() = scheme.onSurface
    val TextSecondary: Color get() = scheme.onSurfaceVariant
    val TextTertiary: Color get() = TextSecondary.copy(alpha = 0.70f)
    val Accent: Color get() = scheme.primary
    val GlassFill: Color get() = scheme.surfaceContainer
    val GlassEdge: Color get() = scheme.outlineVariant
    val Destructive: Color get() = scheme.error

    // Shapes.
    val CoverSmall: Shape = RoundedCornerShape(10.dp)
    val CoverMedium: Shape = RoundedCornerShape(16.dp)
    val CoverLarge: Shape = RoundedCornerShape(24.dp)
    val Card: Shape = RoundedCornerShape(24.dp)
    val Button: Shape = RoundedCornerShape(20.dp)

    private val type = androidx.compose.material3.Typography()
    val LargeTitle: TextStyle get() = type.headlineLarge.copy(color = Text, fontWeight = FontWeight.Bold)
    val Title: TextStyle get() = type.titleLarge.copy(color = Text, fontWeight = FontWeight.SemiBold)
    val Headline: TextStyle get() = type.titleMedium.copy(color = Text)
    val Body: TextStyle get() = type.bodyLarge.copy(color = Text)
    val BodyStrong: TextStyle get() = Body.copy(fontWeight = FontWeight.Medium)
    val Subhead: TextStyle get() = type.bodyMedium.copy(color = TextSecondary)
    val Caption: TextStyle get() = type.bodySmall.copy(color = TextSecondary)
    val Lyric: TextStyle get() = type.headlineMedium.copy(color = Text, fontWeight = FontWeight.Bold)

    // Spacing.
    val Gutter = AppSpacing.Gutter
    val RowHeight = 64.dp
    /** Room the floating tab bar and mini player take at the bottom of every page. */
    val BottomChrome = 180.dp
}

/** A translucent bar or capsule (tab bar, mini player, menus). */
fun Modifier.glass(shape: Shape = CircleShape): Modifier =
    this.background(Immersive.GlassFill, shape).border(1.dp, Immersive.GlassEdge, shape)
