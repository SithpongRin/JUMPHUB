package com.example.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

@Composable
fun ProvideAppLocale(
    languageCode: String,
    content: @Composable () -> Unit
) {
    val currentConfig = LocalConfiguration.current

    val localizedConfig = remember(languageCode, currentConfig) {
        val config = Configuration(currentConfig)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        config.setLocale(locale)
        config
    }

    CompositionLocalProvider(
        LocalConfiguration provides localizedConfig,
        content = content
    )
}
