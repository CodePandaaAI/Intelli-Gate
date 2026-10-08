package com.liftley.intelligate.ui.scan

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.liftley.intelligate.R
import com.liftley.intelligate.domain.Decision
import com.liftley.intelligate.domain.VerificationResult

@Composable
fun ResultPanel(result: VerificationResult) {
    val colors = MaterialTheme.colorScheme
    val (background, foreground) = when (result.decision) {
        Decision.APPROVED -> colors.primaryContainer to colors.onPrimaryContainer
        Decision.DENIED -> colors.errorContainer to colors.onErrorContainer
        Decision.REVIEW -> colors.tertiaryContainer to colors.onTertiaryContainer
    }
    Surface(shape = MaterialTheme.shapes.extraLarge, color = background, contentColor = foreground) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(when (result.decision) {
                Decision.APPROVED -> R.string.approved
                Decision.DENIED -> R.string.not_approved
                Decision.REVIEW -> R.string.unreadable_plate
            }), style = MaterialTheme.typography.headlineMedium)
            result.plateNumber?.let {
                Text(it, style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace)
            }
            val owner = listOfNotNull(result.name, result.role).joinToString(" · ")
            if (owner.isNotEmpty()) Text(owner, style = MaterialTheme.typography.bodyLarge)
            result.hasPass?.let {
                Text(stringResource(if (it) R.string.has_pass else R.string.no_pass))
            }
            if (result.decision == Decision.REVIEW) Text(stringResource(R.string.retake_hint))
        }
    }
}

