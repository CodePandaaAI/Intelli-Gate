package com.liftley.intelligate.ui.scan

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.liftley.intelligate.R

@Composable
fun ErrorPanel(message: String) {
    Text(
        stringResource(R.string.check_failed),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.error,
    )
    Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
}

