package com.example.ui.theme

import androidx.compose.runtime.Composable
import com.example.core.theme.ThemeMode
import com.example.core.theme.UnimaidTheme

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    UnimaidTheme(
        themeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT,
        dynamicColor = dynamicColor,
        content = content
    )
}

