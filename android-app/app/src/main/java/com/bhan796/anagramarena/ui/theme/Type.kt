package com.bhan796.anagramarena.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bhan796.anagramarena.R

val PressStart2P = FontFamily(Font(R.font.press_start_2p_regular))
val Orbitron = FontFamily(Font(R.font.orbitron_bold, FontWeight.Bold))

val Typography = Typography(
    displayLarge = TextStyle(fontFamily = PressStart2P, fontSize = 24.sp, lineHeight = 36.sp, letterSpacing = 2.sp),
    displayMedium = TextStyle(fontFamily = PressStart2P, fontSize = 18.sp, lineHeight = 28.sp, letterSpacing = 2.sp),
    displaySmall = TextStyle(fontFamily = PressStart2P, fontSize = 14.sp, lineHeight = 22.sp, letterSpacing = 1.5.sp),
    headlineLarge = TextStyle(fontFamily = Orbitron, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = 1.sp),
    headlineMedium = TextStyle(fontFamily = Orbitron, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 1.sp),
    headlineSmall = TextStyle(fontFamily = Orbitron, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.8.sp),
    titleLarge = TextStyle(fontFamily = Orbitron, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.5.sp),
    bodyLarge = TextStyle(fontFamily = PressStart2P, fontSize = 10.sp, lineHeight = 18.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = PressStart2P, fontSize = 8.sp, lineHeight = 16.sp),
    bodySmall = TextStyle(fontFamily = PressStart2P, fontSize = 7.sp, lineHeight = 14.sp),
    labelLarge = TextStyle(fontFamily = PressStart2P, fontSize = 9.sp, letterSpacing = 0.8.sp),
    labelMedium = TextStyle(fontFamily = PressStart2P, fontSize = 7.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = PressStart2P, fontSize = 6.sp, letterSpacing = 0.3.sp),
)
