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
import com.arbon.vata.floralens.domain.diagnosis.DiseaseVerifier
import com.arbon.vata.floralens.domain.species.SpeciesIdentifier
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
        val species: String,
        val record: PlantRecord? = null,
    ) : ScanState()
    data class Saved(                                  // Scan persisted to ObjectBox
        val record: PlantRecord
    ) : ScanState()
}

class PlantViewModel(
    private val plantRecordRepository: PlantRecordRepository,
    private val onboardingRepository: OnboardingRepository,
    private val cascadeClassifier: CascadeClassifier,
    private val speciesIdentifier: SpeciesIdentifier,
    private val diseaseVerifier: DiseaseVerifier,
) : ViewModel() {

    /** Read once when the nav graph is built: decides whether Onboarding or the Scan tab starts. */
    val isOnboardingDone: Boolean = onboardingRepository.isDone()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    val history: StateFlow<List<PlantRecord>> =
        plantRecordRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val trackedPlants: StateFlow<List<PlantRecord>> =
        plantRecordRepository.observeTracked()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun completeOnboarding() {
        onboardingRepository.setDone()
    }

    /**
     * Full cascade pipeline: classify on-device, persist the scan with its photo,
     * then — when the gate is unsure — run the cloud second opinions. Pl@ntNet
     * verifies the species or redirects Banana/Avocado/Mango to the v2.0
     * wishlist flow; Plant.id then confirms or corrects the disease verdict.
     * Unavailable cloud stages (offline / no key) leave the on-device result as
     * the final, unverified verdict.
     */
    fun scan(photo: Bitmap) {
        viewModelScope.launch {
            _scanState.update { ScanState.Scanning }
            val verdict = cascadeClassifier.diagnose(photo)
            val parsed = PlantLabels.parse(verdict.prediction.label)
            var record =
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

            if (verdict is CascadeClassifier.Verdict.Confident) {
                _scanState.update { ScanState.OnDeviceResult(record, needsCloud = false) }
                return@launch
            }

            // Needs second opinion: show that step, then merge the cloud answers.
            _scanState.update { ScanState.OnDeviceResult(record, needsCloud = true) }
            val speciesMatch = speciesIdentifier.identify(photo)
            if (speciesMatch != null && !speciesMatch.isSupported) {
                // Species outside v1.0 (Banana/Avocado/Mango): rewrite the
                // record as an unsupported scan feeding the v2.0 wishlist.
                record =
                    plantRecordRepository.save(
                        record.copy(
                            detectedSpecies = speciesMatch.commonName,
                            commonName = speciesMatch.commonName,
                            cloudLabel = speciesMatch.scientificName,
                            isSupported = false,
                            isCloudVerified = true,
                            confidence = speciesMatch.confidence,
                        ),
                    )
                _scanState.update { ScanState.Unsupported(speciesMatch.commonName, record) }
                return@launch
            }

            if (speciesMatch != null) {
                // Cloud agrees the crop is supported: verify the species on the
                // record and keep the higher of the two confidences.
                record =
                    plantRecordRepository.save(
                        record.copy(
                            detectedSpecies = speciesMatch.commonName,
                            commonName = speciesMatch.commonName,
                        ),
                    )
            }

            // Disease-level second opinion (Plant.id): confirms or corrects the
            // on-device disease verdict. Runs even when Pl@ntNet was unavailable
            // — the keys are independent and only the gate decides cost.
            val assessment = diseaseVerifier.assess(photo)
            if (assessment != null) {
                record =
                    plantRecordRepository.save(
                        record.copy(
                            isHealthy = assessment.isHealthy,
                            diseaseName =
                                if (assessment.isHealthy) {
                                    null
                                } else {
                                    assessment.diseaseName ?: record.diseaseName
                                },
                            cloudLabel = assessment.diseaseName ?: "Healthy",
                            isCloudVerified = true,
                            confidence = maxOf(record.confidence, assessment.confidence),
                        ),
                    )
            }
            _scanState.update { ScanState.CloudResult(record) }
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

    fun untrackSpecies(species: String) {
        viewModelScope.launch {
            plantRecordRepository.setTracked(species, tracked = false)
        }
    }

    /** "Notify me when supported": flags the scan record, or files a standalone wishlist entry. */
    fun requestV2Notify(unsupported: ScanState.Unsupported?) {
        viewModelScope.launch {
            val record =
                unsupported?.record?.copy(wantsNotifyV2 = true)
                    ?: PlantRecord(
                        detectedSpecies = unsupported?.species.orEmpty(),
                        commonName = unsupported?.species.orEmpty(),
                        isSupported = false,
                        wantsNotifyV2 = true,
                    )
            plantRecordRepository.save(record)
        }
    }

    /** Persists an unclassifiable gallery import as a marker record. */
    private suspend fun saveScan(
        photoUri: Uri,
        record: PlantRecord,
    ) {
        _scanState.update { ScanState.Scanning }
        val saved = plantRecordRepository.saveScan(photoUri, record)
        _scanState.update { ScanState.Saved(saved) }
    }
}