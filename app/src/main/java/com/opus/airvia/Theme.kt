package com.opus.airvia

import android.graphics.Typeface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Airvia is styled like the faceplate of a hi-fi component: brushed
// aluminium (graphite in the dark), printed ink legends, a green LED that
// means "sound is going out", and one signal yellow for whatever is selected.
// Dynamic color is deliberately off — the hardware look is the identity.

@Immutable
data class AirviaPalette(
    val plate: Color,     // page background
    val panel: Color,     // raised panels
    val groove: Color,    // fader tracks, dividers, inputs
    val ink: Color,       // primary text, primary buttons
    val legend: Color,    // secondary text
    val signal: Color,    // selection
    val onSignal: Color,
    val led: Color,       // streaming
    val ledOff: Color,
    val fault: Color,
)

private val Aluminium = AirviaPalette(
    plate = Color(0xFFE3E4E1),
    panel = Color(0xFFF1F1EE),
    groove = Color(0xFFC9CBC7),
    ink = Color(0xFF1C1D1F),
    legend = Color(0xFF63676A),
    signal = Color(0xFFE8B21E),
    onSignal = Color(0xFF231A00),
    led = Color(0xFF2E9D5B),
    ledOff = Color(0xFFB4B7B3),
    fault = Color(0xFFC23B2E),
)

private val Graphite = AirviaPalette(
    plate = Color(0xFF1D1F21),
    panel = Color(0xFF282A2D),
    groove = Color(0xFF3B3E42),
    ink = Color(0xFFE8E9E5),
    legend = Color(0xFF9A9EA1),
    signal = Color(0xFFF0BE2C),
    onSignal = Color(0xFF231A00),
    led = Color(0xFF4CC57F),
    ledOff = Color(0xFF4A4D51),
    fault = Color(0xFFFF7A6B),
)

val LocalAirviaPalette = staticCompositionLocalOf { Aluminium }

/** Narrow face for readouts (dB, Hz, %, ports) — like printed scale legends. */
val ReadoutFamily = FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))

private fun schemeFor(p: AirviaPalette, dark: Boolean) = (if (dark) darkColorScheme() else lightColorScheme()).copy(
    primary = p.ink,
    onPrimary = p.plate,
    primaryContainer = p.panel,
    onPrimaryContainer = p.ink,
    secondary = p.legend,
    secondaryContainer = p.signal,
    onSecondaryContainer = p.onSignal,
    tertiary = p.led,
    background = p.plate,
    onBackground = p.ink,
    surface = p.plate,
    onSurface = p.ink,
    surfaceVariant = p.panel,
    onSurfaceVariant = p.legend,
    surfaceContainer = p.panel,
    surfaceContainerHigh = p.panel,
    surfaceContainerHighest = p.groove,
    outline = p.groove,
    outlineVariant = p.groove,
    error = p.fault,
    onError = p.plate,
    errorContainer = p.panel,
    onErrorContainer = p.fault,
)

private val AirviaType = Typography().let { t ->
    t.copy(
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Medium, letterSpacing = (-0.5).sp),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Medium, fontSize = 20.sp),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.Medium),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.Medium),
    )
}

val ReadoutStyle = TextStyle(fontFamily = ReadoutFamily, fontSize = 13.sp, letterSpacing = 0.2.sp)

@Composable
fun AirviaTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val palette = if (dark) Graphite else Aluminium
    androidx.compose.runtime.CompositionLocalProvider(LocalAirviaPalette provides palette) {
        MaterialTheme(colorScheme = schemeFor(palette, dark), typography = AirviaType, content = content)
    }
}

object Airvia {
    val palette: AirviaPalette
        @Composable get() = LocalAirviaPalette.current
}
