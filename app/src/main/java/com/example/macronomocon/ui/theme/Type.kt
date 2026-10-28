package com.example.macronomocon.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.macronomocon.R

private const val FONT_PROVIDER_AUTHORITY = "com.google.android.gms.fonts"
private const val FONT_PROVIDER_PACKAGE = "com.google.android.gms"
private const val PUBLIC_SANS = "Public Sans"

/** Downloaded through Google Play services; text falls back to the system sans-serif until it loads. */
private val fontProvider = GoogleFont.Provider(
    providerAuthority = FONT_PROVIDER_AUTHORITY,
    providerPackage = FONT_PROVIDER_PACKAGE,
    certificates = R.array.com_google_android_gms_fonts_certs
)

val PublicSans = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map {
        Font(GoogleFont(PUBLIC_SANS), fontProvider, it)
    }
)

private val Defaults = Typography()

val Typography = Typography(
    displayLarge = Defaults.displayLarge.copy(fontFamily = PublicSans),
    displayMedium = Defaults.displayMedium.copy(fontFamily = PublicSans),
    displaySmall = Defaults.displaySmall.copy(fontFamily = PublicSans),
    headlineLarge = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    headlineSmall = Defaults.headlineSmall.copy(fontFamily = PublicSans),
    titleLarge = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp,
        lineHeight = 29.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 23.sp
    ),
    titleSmall = Defaults.titleSmall.copy(fontFamily = PublicSans),
    bodyLarge = TextStyle(fontFamily = PublicSans, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = PublicSans, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = Defaults.bodySmall.copy(fontFamily = PublicSans),
    labelLarge = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = Defaults.labelMedium.copy(fontFamily = PublicSans),
    labelSmall = Defaults.labelSmall.copy(fontFamily = PublicSans)
)
