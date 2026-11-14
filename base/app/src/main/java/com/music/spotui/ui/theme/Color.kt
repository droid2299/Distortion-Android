package com.music.spotui.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.music.spotui.ui.immersive.Immersive

val AppBackground: Color get() = Immersive.Page
val GridBackground: Color get() = Immersive.Raised
val AppPalette: Color get() = AppAccent
val AppAccent: Color get() = Immersive.Accent
val AppText: Color get() = Immersive.Text
val AppMuted: Color get() = Immersive.TextSecondary
val AppDestructive: Color get() = Immersive.Destructive
/** What a tint is mixed with to make a header. */
val AppShade: Color = Color.Black

/** One of the palette's raised greys (#181818 ... #3E3E3E): "a bit raised from the page". */
fun tone(argb: Long): Color = Color(argb)

/** The cover's colour as a page header: darkened toward black, so the title stays readable. */
fun headerTint(tint: Color, amount: Float): Color = lerp(tint, Color.Black, amount)
