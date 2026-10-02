package com.example.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
fun ProvideAppLocale(
    languageCode: String,
    content: @Composable () -> Unit
) {
    val currentContext = LocalContext.current
    val currentConfig = LocalConfiguration.current

    val localizedContext = remember(languageCode) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(currentContext.resources.configuration)
        config.setLocale(locale)
        currentContext.createConfigurationContext(config)
    }

    val localizedConfig = remember(languageCode, currentConfig) {
        val config = Configuration(currentConfig)
        config.setLocale(Locale(languageCode))
        config
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfig,
        content = content
    )
}
