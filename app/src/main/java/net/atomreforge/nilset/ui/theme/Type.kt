package net.atomreforge.nilset.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import net.atomreforge.nilset.R

val AppFontFamily = FontFamily(
    Font(R.font.jetbrains_maple_mono_medium)
)

private fun appStyle(
    fontFamily: FontFamily,
    fontWeight: FontWeight = FontWeight.Normal,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
) = TextStyle(
    fontFamily = fontFamily,
    fontWeight = fontWeight,
    fontSize = fontSize,
    lineHeight = lineHeight,
)

fun buildTypography(fontFamily: FontFamily): Typography = Typography(
    headlineLarge = appStyle(fontFamily, FontWeight.Bold, 52.sp, 60.sp),
    titleLarge = appStyle(fontFamily, FontWeight.Bold, 20.sp, 28.sp),
    bodyLarge = appStyle(fontFamily, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = appStyle(fontFamily, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = appStyle(fontFamily, fontSize = 13.sp, lineHeight = 20.sp),
    labelMedium = appStyle(fontFamily, fontSize = 14.sp, lineHeight = 20.sp),
)
