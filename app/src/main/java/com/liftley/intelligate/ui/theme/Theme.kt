package com.liftley.intelligate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IntelliGateTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = GateTypography,
        shapes = GateShapes,
        content = content,
    )
}

