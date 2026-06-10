package com.vagell.kv4pht.ui.compose.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vagell.kv4pht.R
import androidx.compose.material3.Typography

val ShareTechMono = FontFamily(
    Font(R.font.share_tech_mono, FontWeight.Normal)
)

val MotoRFARTypography = Typography(
    displayLarge  = TextStyle(fontFamily = ShareTechMono, fontSize = 56.sp, fontWeight = FontWeight.Bold),
    titleMedium   = TextStyle(fontFamily = ShareTechMono, fontSize = 14.sp, letterSpacing = 0.2.sp),
    bodySmall     = TextStyle(fontFamily = ShareTechMono, fontSize = 9.sp,  letterSpacing = 0.15.sp),
    labelSmall    = TextStyle(fontFamily = ShareTechMono, fontSize = 9.sp,  letterSpacing = 0.1.sp),
    bodyMedium    = TextStyle(fontFamily = ShareTechMono, fontSize = 13.sp),
    labelMedium   = TextStyle(fontFamily = ShareTechMono, fontSize = 12.sp),
)
