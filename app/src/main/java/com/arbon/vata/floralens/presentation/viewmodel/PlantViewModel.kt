package com.arbon.vata.floralens.presentation.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbon.vata.floralens.data.objectbox.PlantRecord
import com.arbon.vata.floralens.data.repository.OnboardingRepository
import com.arbon.vata.floralens.data.repository.PlantRecordRepository
import com.arbon.vata.floralens.domain.classifier.CascadeClassifier
import com.arbon.vata.floralens.domain.classifier.PlantLabels
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
    private val cascadeClassifier: CascadeClassifier,
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

    /**
     * Full on-device pipeline: classify, apply the cascade gate, persist the scan
     * with its photo, then emit the verdict the UI navigates on.
     */
    fun scan(photo: Bitmap) {
        viewModelScope.launch {
            _scanState.update { ScanState.Scanning }
            val verdict = cascadeClassifier.diagnose(photo)
            val parsed = PlantLabels.parse(verdict.prediction.label)
            val saved =
                plantRecordRepository.saveScan(
                    photo = photo,
                    record =
                        PlantRecord(
                            detectedSpecies = parsed.species,
                            commonName = parsed.species,
                            diseaseName = parsed.disease,
                            isHealthy = parsed.isHealthy,
                            confidence = verdict.prediction.confidence,
                            onDeviceLabel = verdict.prediction.label,
                        ),
                )
            _scanState.update {
                ScanState.OnDeviceResult(
                    record = saved,
                    needsCloud = verdict is CascadeClassifier.Verdict.NeedsSecondOpinion,
                )
            }
        }
    }

    /** Gallery import: classify the decoded bitmap, or store an unclassifiable marker. */
    fun scan(photoUri: Uri) {
        viewModelScope.launch {
            val bitmap = plantRecordRepository.decode(photoUri)
            if (bitmap != null) {
                scan(bitmap)
            } else {
                saveScan(
                    photoUri,
                    PlantRecord(detectedSpecies = "Unknown", onDeviceLabel = "unreadable_image"),
                )
            }
        }
    }

    fun toggleTracked(record: PlantRecord) {
        viewModelScope.launch {
            plantRecordRepository.save(record.copy(isTracked = !record.isTracked))
        }
    }

    fun saveScan(photo: Bitmap, record: PlantRecord) {
        viewModelScope.launch {
            _scanState.update { ScanState.Scanning }
            val saved = plantRecordRepository.saveScan(photo, record)
            _scanState.update { ScanState.Saved(saved) }
        }
    }

    fun saveScan(photoUri: Uri, record: PlantRecord) {
        viewModelScope.launch {
            _scanState.update { ScanState.Scanning }
            val saved = plantRecordRepository.saveScan(photoUri, record)
            _scanState.update { ScanState.Saved(saved) }
        }
    }
}