package com.liftley.intelligate.data

import com.liftley.intelligate.domain.Decision
import com.liftley.intelligate.domain.VerificationResult

object ResultParser {
    fun parse(values: Map<*, *>): VerificationResult {
        when (values["status"]) {
            "review" -> return VerificationResult(Decision.REVIEW)
            "error" -> error("The server could not finish this check. Take another photo to try again.")
            "complete" -> Unit
            else -> error("The server returned an invalid response.")
        }
        val plate = (values["plate_number"] as? String)?.takeIf(String::isNotBlank)
        requireNotNull(plate) { "The server did not return a plate number." }
        val valid = values["is_valid"] as? Boolean
        requireNotNull(valid) { "The server did not return a valid approval decision." }
        require(values["has_pass"] == null || values["has_pass"] is Boolean) {
            "The server returned an invalid pass status."
        }
        return VerificationResult(
            decision = if (valid) Decision.APPROVED else Decision.DENIED,
            plateNumber = plate,
            name = (values["name"] as? String)?.takeIf(String::isNotBlank),
            role = (values["role"] as? String)?.takeIf(String::isNotBlank),
            hasPass = values["has_pass"] as? Boolean,
        )
    }
}

