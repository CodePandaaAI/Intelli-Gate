package com.liftley.intelligate.ui.components

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.liftley.intelligate.R

enum class GateSymbol(val resource: Int) {
    CAMERA(R.drawable.ic_camera), SHIELD(R.drawable.ic_shield),
}

@Composable
fun GateIcon(symbol: GateSymbol, modifier: Modifier = Modifier) {
    Icon(painterResource(symbol.resource), contentDescription = null, modifier = modifier)
}

