package com.liftley.intelligate.domain

import kotlinx.coroutines.flow.Flow

interface VerificationRepository {
    fun verify(requestId: String, jpeg: ByteArray): Flow<VerificationUpdate>
}

