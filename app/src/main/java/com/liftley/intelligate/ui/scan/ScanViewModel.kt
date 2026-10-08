package com.liftley.intelligate.ui.scan

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liftley.intelligate.data.PlateImageProcessor
import com.liftley.intelligate.domain.VerificationRepository
import com.liftley.intelligate.domain.VerificationUpdate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class ScanViewModel(
    private val repository: VerificationRepository,
    private val images: PlateImageProcessor,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ScanUiState())
    private var cameraUri: Uri? = null
    private var photoUri: Uri? = null
    val state = mutableState.asStateFlow()
    private var verificationJob: Job? = null

    fun createCameraUri(): Uri = images.createCaptureUri().also {
        cameraUri = it
    }

    fun onCameraResult(success: Boolean) {
        val uri = cameraUri ?: return
        cameraUri = null
        if (!success) {
            images.deleteCapture(uri)
            return
        }
        reset()
        photoUri = uri
        verify()
    }

    fun onCameraUnavailable() {
        cameraUri?.let(images::deleteCapture)
        cameraUri = null
        mutableState.value = ScanUiState(stage = ScanStage.ERROR, error = "No camera app could open. Please try again.")
    }

    fun verify() {
        if (state.value.isBusy) return
        val uri = photoUri ?: return
        val requestId = UUID.randomUUID().toString()
        mutableState.value = ScanUiState(stage = ScanStage.PREPARING)
        verificationJob = viewModelScope.launch {
            try {
                withTimeout(60_000.milliseconds) {
                    val jpeg = images.prepare(uri)
                    repository.verify(requestId, jpeg).collect { update ->
                        mutableState.value = when (update) {
                            VerificationUpdate.Uploading -> ScanUiState(stage = ScanStage.UPLOADING)
                            VerificationUpdate.Processing -> ScanUiState(stage = ScanStage.PROCESSING)
                            is VerificationUpdate.Complete -> {
                                clearPhoto()
                                ScanUiState(stage = ScanStage.RESULT, result = update.result)
                            }
                        }
                    }
                }
            } catch (_: TimeoutCancellationException) {
                currentCoroutineContext().ensureActive()
                mutableState.value = ScanUiState(
                    stage = ScanStage.ERROR, canRetry = true,
                    error = "No answer yet. Check your connection and that the Python worker is running.",
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                mutableState.value = ScanUiState(
                    stage = ScanStage.ERROR, canRetry = true,
                    error = error.message ?: "Could not finish the check.",
                )
            }
        }
    }

    fun reset() {
        verificationJob?.cancel()
        clearPhoto()
        mutableState.value = ScanUiState()
    }

    override fun onCleared() {
        clearPhoto()
        cameraUri?.let(images::deleteCapture)
    }

    private fun clearPhoto() {
        photoUri?.let(images::deleteCapture)
        photoUri = null
    }
}

