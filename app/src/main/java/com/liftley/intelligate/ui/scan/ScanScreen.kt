package com.liftley.intelligate.ui.scan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.liftley.intelligate.R
import com.liftley.intelligate.ui.components.GateSymbol
import com.liftley.intelligate.ui.components.LargeActionButton
import com.liftley.intelligate.ui.theme.IntelliGateTheme

@Composable
fun ScanScreen(state: ScanUiState, onCapture: () -> Unit, onRetry: () -> Unit, onReset: () -> Unit) {
    BackHandler(enabled = state.stage != ScanStage.READY, onBack = onReset)
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        bottomBar = {
            Surface(tonalElevation = 1.dp) {
                Box(Modifier.fillMaxWidth().navigationBarsPadding(), contentAlignment = Alignment.Center) {
                    Column(
                        Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        when {
                            state.isBusy -> TextButton(onClick = onReset) { Text(stringResource(R.string.cancel)) }
                            state.stage == ScanStage.ERROR && state.canRetry -> {
                                LargeActionButton(stringResource(R.string.retry_check), GateSymbol.SHIELD, onRetry)
                                TextButton(onClick = onCapture) { Text(stringResource(R.string.retake_photo)) }
                            }
                            else -> LargeActionButton(
                                stringResource(if (state.stage == ScanStage.READY) R.string.scan_number_plate else R.string.scan_another),
                                GateSymbol.CAMERA, onCapture,
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.widthIn(max = 600.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (state.stage) {
                    ScanStage.READY -> WelcomePanel()
                    ScanStage.RESULT -> state.result?.let { ResultPanel(it) }
                    ScanStage.ERROR -> ErrorPanel(state.error ?: stringResource(R.string.check_failed))
                    else -> ProcessingPanel(state.stage)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomePreview() {
    IntelliGateTheme { ScanScreen(ScanUiState(), {}, {}, {}) }
}

