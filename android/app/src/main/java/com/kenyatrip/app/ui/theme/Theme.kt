package com.kenyatrip.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Acacia = Color(0xFF1F5C3A)
val AcaciaDark = Color(0xFF123824)
val Sunset = Color(0xFFE08E2B)
val Sand = Color(0xFFFBF5EC)
val SandDeep = Color(0xFFF1E7D8)
val ChinaRed = Color(0xFFB23A2E)
val Gold = Color(0xFFF4B33E)
val Success = Color(0xFF2E7D4F)
val Danger = Color(0xFFB3261E)
val Info = Color(0xFF2F5D8A)

private val Colors = lightColorScheme(
    primary = Acacia,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFE8D6),
    onPrimaryContainer = AcaciaDark,
    secondary = Sunset,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE3C2),
    onSecondaryContainer = Color(0xFF4A2A00),
    tertiary = ChinaRed,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD4),
    onTertiaryContainer = Color(0xFF410001),
    background = Sand,
    onBackground = Color(0xFF1E1B16),
    surface = Color.White,
    onSurface = Color(0xFF1E1B16),
    surfaceVariant = SandDeep,
    onSurfaceVariant = Color(0xFF5A5246),
    outline = Color(0xFFB8AC9A),
    outlineVariant = Color(0xFFE2D7C6),
    error = Danger,
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun KenyaTripTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = AppTypography, content = content)
}
