package com.example.eeum.ui.theme

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.eeum.R

private val LightColorScheme = lightColorScheme(
    primary = Blue600,
    onPrimary = SurfaceBase,

    secondary = Purple600,
    onSecondary = SurfaceBase,

    tertiary = Orange500,
    onTertiary = SurfaceBase,

    background = SurfaceBase,     // 실제 배경은 아래 Box의 drawable을 사용
    onBackground = Gray800,       // 기본 텍스트 컬러 (#1F2937)

    surface = SurfaceBase,
    onSurface = Gray800,

    surfaceVariant = Gray50,
    outline = Gray300
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
    useDrawableBackground: Boolean = true,
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
        if (useDrawableBackground) {
            // res/drawable/background.xml 을 앱 전역 배경으로 사용
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.background),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
                content()
            }
        } else {
            content()
        }
    }
}
