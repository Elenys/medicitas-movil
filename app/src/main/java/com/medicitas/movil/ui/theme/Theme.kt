package com.medicitas.movil.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Paleta de especificacion/06-sistema-de-diseno.md §1.1 (mismos valores que la web). */
object MediColors {
    val Primary = Color(0xFF1E88E5)
    val PrimaryDark = Color(0xFF1565C0) // fondo de botones primarios (5.75:1 con blanco)
    val PrimaryDarker = Color(0xFF0D47A1)
    val PrimaryLight = Color(0xFFE3F2FD)
    val Success = Color(0xFF2E7D32)
    val SuccessLight = Color(0xFFE8F5E9)
    val Danger = Color(0xFFE53935)
    val DangerDark = Color(0xFFC62828) // fondo de botones de peligro
    val DangerLight = Color(0xFFFFEBEE)
    val Warning = Color(0xFFF9A825)
    val WarningLight = Color(0xFFFFF8E1)
    val Surface = Color(0xFFFFFFFF)
    val Background = Color(0xFFF5F5F5)
    val Border = Color(0xFFE0E0E0)
    val Text = Color(0xFF333333)
    val TextMuted = Color(0xFF757575)
    val TextDisabled = Color(0xFFBDBDBD)
}

private val colors = lightColorScheme(
    primary = MediColors.PrimaryDark,
    onPrimary = Color.White,
    primaryContainer = MediColors.PrimaryLight,
    onPrimaryContainer = MediColors.PrimaryDarker,
    secondary = MediColors.Success,
    onSecondary = Color.White,
    secondaryContainer = MediColors.SuccessLight,
    onSecondaryContainer = Color(0xFF1B5E20),
    error = MediColors.DangerDark,
    onError = Color.White,
    errorContainer = MediColors.DangerLight,
    onErrorContainer = Color(0xFFB71C1C),
    background = MediColors.Background,
    onBackground = MediColors.Text,
    surface = MediColors.Surface,
    onSurface = MediColors.Text,
    surfaceVariant = MediColors.Background,
    onSurfaceVariant = MediColors.TextMuted,
    outline = MediColors.Border,
    outlineVariant = MediColors.Border,
    surfaceContainer = MediColors.Surface,
    surfaceContainerLow = MediColors.Surface,
    surfaceContainerHigh = MediColors.Surface,
)

/** Escala tipográfica de 06 §2 (display 30, h1 24, h2 20, h3 16, body 16, caption 12; mínimo 12). */
private val typography = Typography(
    displaySmall = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
)

@Composable
fun MediCitasTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
