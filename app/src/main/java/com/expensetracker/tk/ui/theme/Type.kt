@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.expensetracker.tk.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.expensetracker.tk.R

/** Plus Jakarta Sans variable font (wght 200–800), tabular figures for aligned amounts. */
val PlusJakarta = FontFamily(
    Font(
        R.font.plus_jakarta_sans, weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        R.font.plus_jakarta_sans, weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        R.font.plus_jakarta_sans, weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        R.font.plus_jakarta_sans, weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    ),
    Font(
        R.font.plus_jakarta_sans, weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))
    )
)

private const val TNUM = "tnum"

val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = PlusJakarta, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 36.sp, fontFeatureSettings = TNUM),
    headlineMedium = TextStyle(fontFamily = PlusJakarta, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 30.sp, fontFeatureSettings = TNUM),
    headlineSmall = TextStyle(fontFamily = PlusJakarta, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 26.sp, fontFeatureSettings = TNUM),
    titleLarge = TextStyle(fontFamily = PlusJakarta, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp, fontFeatureSettings = TNUM),
    titleMedium = TextStyle(fontFamily = PlusJakarta, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 21.sp, fontFeatureSettings = TNUM),
    titleSmall = TextStyle(fontFamily = PlusJakarta, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, fontFeatureSettings = TNUM),
    bodyLarge = TextStyle(fontFamily = PlusJakarta, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp, fontFeatureSettings = TNUM),
    bodyMedium = TextStyle(fontFamily = PlusJakarta, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp, fontFeatureSettings = TNUM),
    bodySmall = TextStyle(fontFamily = PlusJakarta, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 15.sp, fontFeatureSettings = TNUM),
    labelLarge = TextStyle(fontFamily = PlusJakarta, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp, fontFeatureSettings = TNUM),
    labelMedium = TextStyle(fontFamily = PlusJakarta, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, fontFeatureSettings = TNUM),
    labelSmall = TextStyle(fontFamily = PlusJakarta, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, lineHeight = 13.sp, fontFeatureSettings = TNUM)
)
