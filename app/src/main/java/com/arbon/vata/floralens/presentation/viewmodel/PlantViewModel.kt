package com.arbon.vata.floralens.presentation.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbon.vata.floralens.data.objectbox.PlantRecord
import com.arbon.vata.floralens.data.repository.OnboardingRepository
import com.arbon.vata.floralens.data.repository.PlantRecordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class ScanState {
    object Idle : ScanState()                          // Nothing happening
    object Scanning : ScanState()                      // TFLite is running
    data class OnDeviceResult(                         // TFLite finished
        val record: PlantRecord,
        val needsCloud: Boolean                        // true if confidence <0.80
    ) : ScanState()
    data class CloudResult(                            // Pl@ntNet/Gemini second opinion done
        val record: PlantRecord
    ) : ScanState()
    data class Unsupported(                            // Banana / Avocado detected
        val species: String
    ) : ScanState()
    data class Saved(                                  // Scan persisted to ObjectBox
        val record: PlantRecord
    ) : ScanState()
}

class PlantViewModel(
    private val plantRecordRepository: PlantRecordRepository,
    private val onboardingRepository: OnboardingRepository,
) : ViewModel() {

    /** Read once when the nav graph is built: decides whether Onboarding or the Scan tab starts. */
    val isOnboardingDone: Boolean = onboardingRepository.isDone()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    val history: StateFlow<List<PlantRecord>> =
        plantRecordRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun completeOnboarding() {
        onboardingRepository.setDone()
    }

    fun saveScan(photo: Bitmap, record: PlantRecord) {
        viewModelScope.launch {
            val saved = plantRecordRepository.saveScan(photo, record)
            _scanState.update { ScanState.Saved(saved) }
        }
    }

    fun saveScan(photoUri: Uri, record: PlantRecord) {
        viewModelScope.launch {
            val saved = plantRecordRepository.saveScan(photoUri, record)
            _scanState.update { ScanState.Saved(saved) }
        }
    }
}