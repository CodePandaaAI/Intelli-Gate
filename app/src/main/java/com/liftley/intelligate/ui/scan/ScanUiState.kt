package com.liftley.intelligate.ui.scan

import com.liftley.intelligate.domain.VerificationResult

enum class ScanStage { READY, PREPARING, UPLOADING, PROCESSING, RESULT, ERROR }

data class ScanUiState(
    val stage: ScanStage = ScanStage.READY,
    val result: VerificationResult? = null,
    val error: String? = null,
    val canRetry: Boolean = false,
) {
    val isBusy get() = stage == ScanStage.PREPARING || stage == ScanStage.UPLOADING || stage == ScanStage.PROCESSING
}

