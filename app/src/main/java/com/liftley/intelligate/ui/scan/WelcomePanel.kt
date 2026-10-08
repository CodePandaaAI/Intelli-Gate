package com.liftley.intelligate.ui.scan

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.liftley.intelligate.R

@Composable
fun WelcomePanel() {
    Text(
        text = stringResource(R.string.scan_hint),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
}

