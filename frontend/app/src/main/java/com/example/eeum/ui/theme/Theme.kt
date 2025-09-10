package com.example.eeum.ui.theme

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTonalElevationEnabled
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.eeum.R

private val LightColorScheme = lightColorScheme(
    // Primary colors - 버튼, FAB 등에 사용
    primary = Blue600,
    onPrimary = SurfaceBase,
    primaryContainer = Blue100, // Primary 컨테이너 (연한 파란색)
    onPrimaryContainer = Blue600,

    // Secondary colors - 보조 요소들
    secondary = Purple600,
    onSecondary = SurfaceBase,
    secondaryContainer = Purple100, // Secondary 컨테이너 (연한 보라색)
    onSecondaryContainer = Purple600,

    // Tertiary colors - 액센트 요소들
    tertiary = Orange500,
    onTertiary = SurfaceBase,
    tertiaryContainer = Yellow200, // Tertiary 컨테이너 (연한 노란색)
    onTertiaryContainer = Orange500,

    // Background colors
    background = BackgroundColor, // 배경과 동일한 색상
    onBackground = Gray800,

    // Surface colors - 모든 Surface 요소를 순수한 흰색으로
    surface = SurfaceBase,
    onSurface = Gray800,
    surfaceContainer = SurfaceBase,
    surfaceContainerLow = SurfaceBase,
    surfaceContainerLowest = SurfaceBase,
    surfaceContainerHigh = SurfaceBase,
    surfaceContainerHighest = SurfaceBase,
    surfaceBright = SurfaceBase,
    surfaceDim = SurfaceBase,
    surfaceTint = Blue600, // Surface tint는 primary 색상 사용

    // Variant colors
    surfaceVariant = Gray50,
    onSurfaceVariant = Gray600,

    // Outline colors - 경계선 등
    outline = Gray300,
    outlineVariant = Gray50,

    // Inverse colors - 다크모드 대비용
    inverseSurface = Gray900,
    inversePrimary = Blue500,

    // Scrim - 모달, 드롭다운 배경
    scrim = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.32f),

    // Error colors
    error = Red500,
    onError = SurfaceBase,
    errorContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD6),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFF410002)
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue500,
    onPrimary = SurfaceBase,

    secondary = Purple600,
    onSecondary = SurfaceBase,

    tertiary = Orange500,
    onTertiary = SurfaceBase,

    background = Gray900,
    onBackground = SurfaceBase,

    surface = Gray900,
    onSurface = SurfaceBase,

    surfaceVariant = Gray600,
    outline = Gray400
)

@Composable
fun EeumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // 고정 팔레트 사용 권장: false
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography
    ) {
        CompositionLocalProvider(LocalTonalElevationEnabled provides false) {
            // 전역 배경을 theme background 색으로만 칠함 (단색, drawable 미사용)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                content()
            }
        }
    }
}
