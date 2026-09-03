package com.ridvan.sadehesap.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AcikRenkSemasi = lightColorScheme(
    primary = AcikBirincil,
    onPrimary = AcikBirincilUzerine,
    primaryContainer = AcikBirincilKapsayici,
    onPrimaryContainer = AcikBirincilKapsayiciUzerine,
    secondary = AcikIkincil,
    onSecondary = AcikIkincilUzerine,
    secondaryContainer = AcikIkincilKapsayici,
    onSecondaryContainer = AcikIkincilKapsayiciUzerine,
    tertiary = AcikUcuncul,
    onTertiary = AcikUcunculUzerine,
    tertiaryContainer = AcikUcunculKapsayici,
    onTertiaryContainer = AcikUcunculKapsayiciUzerine,
    error = AcikHata,
    onError = AcikHataUzerine,
    errorContainer = AcikHataKapsayici,
    onErrorContainer = AcikHataKapsayiciUzerine,
    background = AcikArkaPlan,
    onBackground = AcikArkaPlanUzerine,
    surface = AcikYuzey,
    onSurface = AcikYuzeyUzerine,
    surfaceVariant = AcikYuzeyVaryant,
    onSurfaceVariant = AcikYuzeyVaryantUzerine,
    outline = AcikAnahat
)

private val KoyuRenkSemasi = darkColorScheme(
    primary = KoyuBirincil,
    onPrimary = KoyuBirincilUzerine,
    primaryContainer = KoyuBirincilKapsayici,
    onPrimaryContainer = KoyuBirincilKapsayiciUzerine,
    secondary = KoyuIkincil,
    onSecondary = KoyuIkincilUzerine,
    secondaryContainer = KoyuIkincilKapsayici,
    onSecondaryContainer = KoyuIkincilKapsayiciUzerine,
    tertiary = KoyuUcuncul,
    onTertiary = KoyuUcunculUzerine,
    tertiaryContainer = KoyuUcunculKapsayici,
    onTertiaryContainer = KoyuUcunculKapsayiciUzerine,
    error = KoyuHata,
    onError = KoyuHataUzerine,
    errorContainer = KoyuHataKapsayici,
    onErrorContainer = KoyuHataKapsayiciUzerine,
    background = KoyuArkaPlan,
    onBackground = KoyuArkaPlanUzerine,
    surface = KoyuYuzey,
    onSurface = KoyuYuzeyUzerine,
    surfaceVariant = KoyuYuzeyVaryant,
    onSurfaceVariant = KoyuYuzeyVaryantUzerine,
    outline = KoyuAnahat
)

/**
 * Sade Hesap uygulama teması. Android 12+ üzerinde dinamik renk (duvar kâğıdından türetilen
 * Material You paleti) kullanılır; öncesinde sabit yeşil tohum paletine düşer. Sistem
 * açık/koyu tema tercihini izler ve edge-to-edge için durum/gezinme çubuğu ikon rengini ayarlar.
 */
@Composable
fun SadeHesapTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val koyuTema = isSystemInDarkTheme()
    val context = LocalContext.current
    val renkSemasi = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (koyuTema) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        koyuTema -> KoyuRenkSemasi
        else -> AcikRenkSemasi
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val pencere = (view.context as Activity).window
            val denetleyici = WindowCompat.getInsetsController(pencere, view)
            denetleyici.isAppearanceLightStatusBars = !koyuTema
            denetleyici.isAppearanceLightNavigationBars = !koyuTema
        }
    }

    MaterialTheme(
        colorScheme = renkSemasi,
        typography = SadeHesapTypography,
        content = content
    )
}
