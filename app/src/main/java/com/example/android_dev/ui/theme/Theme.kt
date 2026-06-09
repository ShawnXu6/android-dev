package com.example.android_dev.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 主题配色方案功能：可在「粉蓝」与「经典青绿」之间切换。
enum class AppPalette(val label: String) {
    PINK_BLUE("粉蓝"),
    TEAL("青绿");

    companion object {
        fun fromName(name: String?): AppPalette =
            entries.firstOrNull { it.name == name } ?: PINK_BLUE
    }
}

// ===== 粉蓝主题 =====
private val PinkBlueDark = darkColorScheme(
    primary = Pink80,
    onPrimary = Color(0xFF4A0E2A),
    secondary = Blue80,
    onSecondary = Color(0xFF0E2347),
    tertiary = Lavender80,
    primaryContainer = Color(0xFF6E2049),
    onPrimaryContainer = Color(0xFFFBD9E7),
    secondaryContainer = Color(0xFF1E3A66),
    onSecondaryContainer = Color(0xFFD7E4FB),
    background = Color(0xFF181216),
    surface = Color(0xFF211A1E),
    surfaceVariant = Color(0xFF352B30)
)

private val PinkBlueLight = lightColorScheme(
    primary = Pink40,
    onPrimary = Color(0xFFFFFFFF),
    secondary = Blue40,
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Lavender40,
    background = Color(0xFFFFF5F9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF3E6EE),
    primaryContainer = Color(0xFFFBD9E7),
    onPrimaryContainer = Color(0xFF5E0A36),
    secondaryContainer = Color(0xFFDCE7FB),
    onSecondaryContainer = Color(0xFF11305E),
    tertiaryContainer = Color(0xFFEADDF6)
)

// ===== 经典青绿主题 =====
private val TealDark = darkColorScheme(
    primary = Teal80,
    secondary = Steel80,
    tertiary = Coral80,
    background = Color(0xFF111714),
    surface = Color(0xFF18211D),
    surfaceVariant = Color(0xFF27332F)
)

private val TealLight = lightColorScheme(
    primary = Teal40,
    secondary = Steel40,
    tertiary = Coral40,
    background = Color(0xFFF6F7F2),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE6EBE4),
    primaryContainer = Color(0xFFDCEFE8),
    secondaryContainer = Color(0xFFE0E7F6),
    tertiaryContainer = Color(0xFFF8E1DC)
)

@Composable
fun AndroiddevTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    palette: AppPalette = AppPalette.PINK_BLUE,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        palette == AppPalette.TEAL -> if (darkTheme) TealDark else TealLight
        else -> if (darkTheme) PinkBlueDark else PinkBlueLight
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
