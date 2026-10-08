package com.liftley.intelligate.ui.scan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ScanRoute(viewModel: ScanViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), viewModel::onCameraResult)
    ScanScreen(
        state = state,
        onCapture = {
            try {
                camera.launch(viewModel.createCameraUri())
            } catch (_: Exception) {
                viewModel.onCameraUnavailable()
            }
        },
        onRetry = viewModel::verify,
        onReset = viewModel::reset,
    )
}

