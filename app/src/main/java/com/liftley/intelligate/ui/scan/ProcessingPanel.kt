package com.liftley.intelligate.ui.scan

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.liftley.intelligate.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProcessingPanel(stage: ScanStage) {
    LoadingIndicator(Modifier.size(72.dp))
    Text(
        stringResource(when (stage) {
            ScanStage.PREPARING -> R.string.preparing_photo
            ScanStage.UPLOADING -> R.string.sending_photo
            else -> R.string.checking_vehicle
        }),
        style = MaterialTheme.typography.titleLarge,
    )
}

