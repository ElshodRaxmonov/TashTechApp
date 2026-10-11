package com.mrdev.tashtech.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mrdev.tashtech.R

// One variable font file; weights are resolved via variation settings (needs API 26+, our minSdk).
@OptIn(ExperimentalTextApi::class)
private fun manrope(weight: FontWeight) = Font(
    resId = R.font.manrope,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val ManropeFamily = FontFamily(
    manrope(FontWeight.Normal),
    manrope(FontWeight.Medium),
    manrope(FontWeight.SemiBold),
    manrope(FontWeight.Bold),
    manrope(FontWeight.ExtraBold),
)

private fun TextStyle.m(
    size: TextUnit,
    line: TextUnit,
    weight: FontWeight,
    tracking: TextUnit = 0.sp,
) = copy(
    fontFamily = ManropeFamily,
    fontSize = size,
    lineHeight = line,
    fontWeight = weight,
    letterSpacing = tracking,
)

private val base = Typography()

/** Compact scale: ExtraBold/Bold headings, SemiBold/Medium metadata, Normal body. */
val TashTechTypography = Typography(
    displayLarge = base.displayLarge.m(44.sp, 52.sp, FontWeight.ExtraBold, (-0.5).sp),
    displayMedium = base.displayMedium.m(38.sp, 46.sp, FontWeight.ExtraBold, (-0.25).sp),
    displaySmall = base.displaySmall.m(32.sp, 40.sp, FontWeight.ExtraBold),
    headlineLarge = base.headlineLarge.m(28.sp, 36.sp, FontWeight.ExtraBold),
    headlineMedium = base.headlineMedium.m(24.sp, 32.sp, FontWeight.Bold),
    headlineSmall = base.headlineSmall.m(20.sp, 28.sp, FontWeight.Bold),
    titleLarge = base.titleLarge.m(18.sp, 26.sp, FontWeight.Bold),
    titleMedium = base.titleMedium.m(16.sp, 22.sp, FontWeight.SemiBold, 0.1.sp),
    titleSmall = base.titleSmall.m(14.sp, 20.sp, FontWeight.SemiBold, 0.1.sp),
    bodyLarge = base.bodyLarge.m(16.sp, 24.sp, FontWeight.Normal, 0.1.sp),
    bodyMedium = base.bodyMedium.m(14.sp, 20.sp, FontWeight.Normal, 0.15.sp),
    bodySmall = base.bodySmall.m(12.sp, 16.sp, FontWeight.Normal, 0.2.sp),
    labelLarge = base.labelLarge.m(14.sp, 20.sp, FontWeight.SemiBold, 0.1.sp),
    labelMedium = base.labelMedium.m(12.sp, 16.sp, FontWeight.SemiBold, 0.3.sp),
    labelSmall = base.labelSmall.m(11.sp, 16.sp, FontWeight.Medium, 0.3.sp),
)
