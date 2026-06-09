package `in`.co.oreznet.missionuniform.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


private val DefaultColorScheme = lightColorScheme(
    background = Color(0xFF111827),
    surface = Color(0xFFDDB049),
    primary = Color(0xFFF9FAFB),
    secondary = Color(0xFF6B7280),
    tertiary = Color(0xFF9CA3AF)
//    onPrimary = Color.White,
//    onSecondary = Color.White,
//    onTertiary = Color.White,
//    onBackground = Color(0xFF1C1B1F),
//    onSurface = Color(0xFF1C1B1F),
)

@Composable
fun MissionUniformTheme(
//    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
//    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = DefaultColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}