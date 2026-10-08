package com.liftley.intelligate.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LargeActionButton(label: String, symbol: GateSymbol, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = ButtonDefaults.LargeContainerHeight),
        shapes = ButtonDefaults.shapes(),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 20.dp),
    ) {
        GateIcon(symbol, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}

