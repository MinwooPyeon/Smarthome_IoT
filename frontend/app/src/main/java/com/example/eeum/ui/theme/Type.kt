package com.example.eeum.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.eeum.R

// res/font/goormsansregular.ttf 등 파일명을 사용합니다.
val GoormSans = FontFamily(
    Font(R.font.goormsansregular, weight = FontWeight.Normal),
    Font(R.font.goormsansmedium,  weight = FontWeight.Medium),
    Font(R.font.goormsansbold,    weight = FontWeight.Bold),
)

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = GoormSans, fontWeight = FontWeight.Bold,   fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium= TextStyle(fontFamily = GoormSans, fontWeight = FontWeight.Bold,   fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = GoormSans, fontWeight = FontWeight.Bold,   fontSize = 36.sp, lineHeight = 44.sp),

    headlineLarge= TextStyle(fontFamily = GoormSans, fontWeight = FontWeight.Bold,   fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium=TextStyle(fontFamily = GoormSans, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall= TextStyle(fontFamily = GoormSans, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 32.sp),

    titleLarge   = TextStyle(fontFamily = GoormSans, fontWeight = FontWeight.Bold,   fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium  = TextStyle(fontFamily = GoormSans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall   = TextStyle(fontFamily = GoormSans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),

    bodyLarge    = TextStyle(fontFamily = GoormSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium   = TextStyle(fontFamily = GoormSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall    = TextStyle(fontFamily = GoormSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),

    labelLarge   = TextStyle(fontFamily = GoormSans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium  = TextStyle(fontFamily = GoormSans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall   = TextStyle(fontFamily = GoormSans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)
