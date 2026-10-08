package com.liftley.intelligate.domain

enum class Decision { APPROVED, DENIED, REVIEW }

data class VerificationResult(
    val decision: Decision,
    val plateNumber: String? = null,
    val name: String? = null,
    val role: String? = null,
    val hasPass: Boolean? = null,
)

sealed interface VerificationUpdate {
    data object Uploading : VerificationUpdate
    data object Processing : VerificationUpdate
    data class Complete(val result: VerificationResult) : VerificationUpdate
}

