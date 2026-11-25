package com.droidexplorer.ui.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Windows 11 type ramp (size/line height in px at 100% scale, used here as sp):
 * Caption 12/16, Body 14/20, Body Strong 14/20 semibold, Body Large 18/24,
 * Subtitle 20/28 semibold, Title 28/36 semibold, Title Large 40/52 semibold, Display 68/92 semibold.
 *
 * Segoe UI Variable is not licensed for redistribution, so [FluentFontFamily] is the single place
 * where an open, metric-similar font will be plugged in (candidate: Selawik, SIL OFL; Hebrew coverage to be verified).
 */
val FluentFontFamily: FontFamily = FontFamily.SansSerif

@Immutable
class FluentTypography(
    val caption: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val bodyLarge: TextStyle,
    val subtitle: TextStyle,
    val title: TextStyle,
    val titleLarge: TextStyle,
    val display: TextStyle,
)

private fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FluentFontFamily,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
)

fun fluentTypography() = FluentTypography(
    caption = style(12, 16),
    body = style(14, 20),
    bodyStrong = style(14, 20, FontWeight.SemiBold),
    bodyLarge = style(18, 24),
    subtitle = style(20, 28, FontWeight.SemiBold),
    title = style(28, 36, FontWeight.SemiBold),
    titleLarge = style(40, 52, FontWeight.SemiBold),
    display = style(68, 92, FontWeight.SemiBold),
)
