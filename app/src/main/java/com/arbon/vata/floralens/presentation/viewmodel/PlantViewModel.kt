package com.arbon.vata.floralens.presentation.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbon.vata.floralens.data.objectbox.PlantRecord
import com.arbon.vata.floralens.data.objectbox.UNKNOWN_DIAGNOSIS_LABEL_PREFIX
import com.arbon.vata.floralens.data.objectbox.USER_CORRECTED_LABEL_PREFIX
import com.arbon.vata.floralens.data.objectbox.USER_CORRECTED_UNKNOWN_LABEL_PREFIX
import com.arbon.vata.floralens.data.objectbox.classifierLabel
import com.arbon.vata.floralens.data.repository.OnboardingRepository
import com.arbon.vata.floralens.data.repository.PlantRecordRepository
import com.arbon.vata.floralens.domain.classifier.CascadeClassifier
import com.arbon.vata.floralens.domain.classifier.PlantLabels
import com.arbon.vata.floralens.domain.diagnosis.DiseaseAssessment
import com.arbon.vata.floralens.domain.diagnosis.DiseaseVerifier
import com.arbon.vata.floralens.domain.photos.PhotoRegion
import com.arbon.vata.floralens.domain.species.SpeciesIdentifier
import com.arbon.vata.floralens.domain.species.SpeciesMatch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class ScanState {
    object Idle : ScanState()                          // Nothing happening
    data class Failed(val message: String) : ScanState()
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
}

data class CloudVerificationState(
    val recordId: Long,
    val isRunning: Boolean,
    val message: String? = null,
)

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

    private var retryScan: (() -> Unit)? = null

    private val deletedRecordIds = mutableSetOf<Long>()

    private val diagnosisRevisions = mutableMapOf<Long, Long>()

    private val _cloudVerificationState = MutableStateFlow<CloudVerificationState?>(null)
    val cloudVerificationState: StateFlow<CloudVerificationState?> = _cloudVerificationState.asStateFlow()

    val history: StateFlow<List<PlantRecord>> =
        plantRecordRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val requestedWishlistSpecies: StateFlow<Set<String>> =
        history
            .map { records -> records.filter { it.wantsNotifyV2 }.map { it.detectedSpecies.lowercase() }.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

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
    fun scan(photo: Bitmap, focusRegion: PhotoRegion? = null) {
        launchScan(retry = { scan(photo, focusRegion) }) { performScan(photo, focusRegion) }
    }

    private suspend fun performScan(photo: Bitmap, focusRegion: PhotoRegion? = null) {
        val region = focusRegion?.clampedTo(photo.width, photo.height)
        val modelInput =
            if (region == null) photo else Bitmap.createBitmap(
                photo, region.left, region.top, region.right - region.left, region.bottom - region.top,
            )
        val verdict =
            try {
                cascadeClassifier.diagnose(modelInput)
            } finally {
                if (modelInput !== photo) modelInput.recycle()
            }
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
            return
        }

        // Needs second opinion: show that step, then merge the cloud answers.
        _scanState.update { ScanState.OnDeviceResult(record, needsCloud = true) }
        val speciesMatch = speciesIdentifier.identify(photo)
        if (record.id in deletedRecordIds) return
        if (speciesMatch != null) {
            record = plantRecordRepository.save(record.applyCloudResults(speciesMatch, null))
            if (!speciesMatch.isSupported) {
                _scanState.value = ScanState.Unsupported(speciesMatch.commonName, record)
                return
            }
        }

        // Disease-level second opinion (Plant.id): confirms or corrects the
        // on-device disease verdict. Runs even when Pl@ntNet was unavailable
        // — the keys are independent and only the gate decides cost.
        val assessment = diseaseVerifier.assess(photo)
        if (record.id in deletedRecordIds) return
        if (assessment != null) {
            record = plantRecordRepository.save(record.applyCloudResults(null, assessment))
        }
        _scanState.update { ScanState.CloudResult(record) }
    }

    /** Gallery imports use the same error handling and retry path as camera scans. */
    fun scan(photoUri: Uri) {
        launchScan(retry = { scan(photoUri) }) {
            val bitmap = plantRecordRepository.decode(photoUri)
                ?: throw IllegalArgumentException("Unreadable image")
            performScan(bitmap)
        }
    }

    fun retryFailedScan() {
        if (_scanState.value is ScanState.Failed) retryScan?.invoke()
    }

    private fun launchScan(retry: () -> Unit, operation: suspend () -> Unit) {
        retryScan = retry
        // Clear the previous result before navigation can observe it.
        _scanState.value = ScanState.Scanning
        viewModelScope.launch {
            try {
                operation()
                retryScan = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _scanState.value = ScanState.Failed("Could not analyze or save this photo. Try again or choose another photo.")
            }
        }
    }

    suspend fun deleteRecord(record: PlantRecord): Boolean =
        try {
            plantRecordRepository.delete(record.id)
            deletedRecordIds.add(record.id)
            _scanState.update { current ->
                val activeId = when (current) {
                    is ScanState.OnDeviceResult -> current.record.id
                    is ScanState.CloudResult -> current.record.id
                    is ScanState.Unsupported -> current.record?.id
                    else -> null
                }
                if (activeId == record.id) ScanState.Idle else current
            }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
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

    fun toggleWishlistRequest(species: String) {
        viewModelScope.launch {
            plantRecordRepository.toggleWishlistRequest(species)
        }
    }

    /** Saves a user's corrected supported-plant diagnosis for this scan. */
    fun correctDiagnosis(
        record: PlantRecord,
        correctedSpecies: String,
        correctedLabel: String?,
    ) {
        if (correctedSpecies !in PlantLabels.SUPPORTED_SPECIES) return
        val parsed = correctedLabel?.let { PlantLabels.parse(it) }
        if (parsed != null && parsed.species != correctedSpecies) return
        diagnosisRevisions[record.id] = (diagnosisRevisions[record.id] ?: 0L) + 1

        viewModelScope.launch {
            val corrected =
                plantRecordRepository.save(
                    record.copy(
                        detectedSpecies = correctedSpecies,
                        commonName = correctedSpecies,
                        diseaseName = parsed?.disease,
                        isHealthy = parsed?.isHealthy ?: false,
                        onDeviceLabel =
                            if (parsed == null) {
                                USER_CORRECTED_UNKNOWN_LABEL_PREFIX + correctedSpecies
                            } else {
                                USER_CORRECTED_LABEL_PREFIX + correctedLabel.orEmpty()
                            },
                        cloudLabel = null,
                        isCloudVerified = false,
                        hasHealthAssessment = false,
                    ),
                )
            _scanState.update { current ->
                when (current) {
                    is ScanState.OnDeviceResult ->
                        if (current.record.id == corrected.id) ScanState.OnDeviceResult(corrected, needsCloud = false) else current
                    is ScanState.CloudResult ->
                        if (current.record.id == corrected.id) ScanState.OnDeviceResult(corrected, needsCloud = false) else current
                    else -> current
                }
            }
        }
    }

    /** Rechecks a saved scan with the cloud services even when the on-device model was confident. */
    fun verifyWithCloud(record: PlantRecord) {
        startCloudVerification(record, identifySpecies = true)
    }

    /** Requests a Plant.id health assessment for a plant outside the offline model's supported classes. */
    fun verifyUnsupportedWithCloud(record: PlantRecord) {
        startCloudVerification(record, identifySpecies = false)
    }

    private fun startCloudVerification(
        record: PlantRecord,
        identifySpecies: Boolean,
    ) {
        val alreadyRunning = _cloudVerificationState.value?.let { it.recordId == record.id && it.isRunning } == true
        if (alreadyRunning) return

        val photoPath = record.photoPath
        if (photoPath == null) {
            setCloudVerificationResult(record.id, "This scan has no saved photo to verify.")
            return
        }

        val diagnosisRevision = diagnosisRevisions[record.id] ?: 0L
        _cloudVerificationState.value = CloudVerificationState(record.id, isRunning = true)
        viewModelScope.launch {
            try {
                val photo = plantRecordRepository.decodeSavedPhoto(photoPath)
                if (photo == null) {
                    setCloudVerificationResult(record.id, "The saved photo could not be opened.")
                    return@launch
                }

                val speciesMatch = if (identifySpecies) speciesIdentifier.identify(photo) else null
                val diseaseAssessment = diseaseVerifier.assess(photo)

                // The cloud calls run for seconds, during which the user may edit
                // the scan (correction, tracking). Cloud fields must merge onto
                // the row as it exists now, not the snapshot from button-tap time.
                val current = plantRecordRepository.get(record.id)
                if (current == null) {
                    setCloudVerificationResult(record.id, "This scan no longer exists.")
                    return@launch
                }
                if ((diagnosisRevisions[record.id] ?: 0L) != diagnosisRevision) {
                    setCloudVerificationResult(record.id, "Your correction was kept. Verify again to request a new assessment.")
                    return@launch
                }
                val saved = plantRecordRepository.save(current.applyCloudResults(speciesMatch, diseaseAssessment))

                updateActiveScan(saved)
                setCloudVerificationResult(saved.id, verificationMessage(speciesMatch, diseaseAssessment, identifySpecies))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                setCloudVerificationResult(record.id, "Cloud verification failed. Please try again.")
            }
        }
    }

    private fun PlantRecord.applyCloudResults(
        speciesMatch: SpeciesMatch?,
        diseaseAssessment: DiseaseAssessment?,
    ): PlantRecord {
        var updated = this
        if (speciesMatch != null) {
            // PlantNetClient maps scientific names to the same canonical crop names as PlantLabels.
            val speciesChanged = !speciesMatch.commonName.equals(updated.detectedSpecies, ignoreCase = true)
            if (speciesChanged) {
                updated = updated.copy(
                    diseaseName = null,
                    isHealthy = false,
                    isCloudVerified = false,
                    hasHealthAssessment = false,
                    onDeviceLabel = UNKNOWN_DIAGNOSIS_LABEL_PREFIX + updated.classifierLabel(),
                    confidence = 0f,
                )
            }
            updated =
                updated.copy(
                    detectedSpecies = speciesMatch.commonName,
                    commonName = speciesMatch.commonName,
                    isSupported = speciesMatch.isSupported,
                    cloudLabel = speciesMatch.scientificName,
                    confidence = if (speciesMatch.isSupported) updated.confidence else speciesMatch.confidence,
                )
        }
        if (diseaseAssessment != null && speciesMatch?.isSupported != false) {
            updated =
                updated.copy(
                    isHealthy = diseaseAssessment.isHealthy,
                    diseaseName = if (diseaseAssessment.isHealthy) null else diseaseAssessment.diseaseName,
                    cloudLabel =
                        if (updated.isSupported) diseaseAssessment.diseaseName ?: "Healthy"
                        else updated.cloudLabel,
                    isCloudVerified = true,
                    hasHealthAssessment = true,
                    confidence = if (updated.isSupported) diseaseAssessment.confidence else updated.confidence,
                    onDeviceLabel =
                        if (updated.isSupported) {
                            updated.onDeviceLabel
                                .removePrefix(UNKNOWN_DIAGNOSIS_LABEL_PREFIX)
                                .removePrefix(USER_CORRECTED_LABEL_PREFIX)
                                .removePrefix(USER_CORRECTED_UNKNOWN_LABEL_PREFIX)
                        } else {
                            updated.onDeviceLabel
                        },
                )
        }
        return updated
    }

    private fun verificationMessage(
        speciesMatch: SpeciesMatch?,
        diseaseAssessment: DiseaseAssessment?,
        identifySpecies: Boolean,
    ): String =
        when {
            speciesMatch != null && !speciesMatch.isSupported ->
                "Cloud identified ${speciesMatch.commonName}, which is outside supported diagnoses."
            diseaseAssessment != null -> "Cloud health assessment received."
            speciesMatch != null -> "Plant species checked; disease verification was unavailable."
            identifySpecies -> "Cloud verification was unavailable. Check your connection and API setup."
            else -> "No cloud health assessment was returned. Check your connection, API key, and credits."
        }

    private fun updateActiveScan(record: PlantRecord) {
        _scanState.update { current ->
            when (current) {
                is ScanState.OnDeviceResult ->
                    if (current.record.id == record.id) {
                        if (record.isSupported) ScanState.OnDeviceResult(record, needsCloud = false)
                        else ScanState.Unsupported(record.commonName, record)
                    } else current
                is ScanState.CloudResult ->
                    if (current.record.id == record.id) {
                        if (record.isSupported) ScanState.CloudResult(record)
                        else ScanState.Unsupported(record.commonName, record)
                    } else current
                is ScanState.Unsupported ->
                    if (current.record?.id == record.id) ScanState.Unsupported(record.commonName, record) else current
                else -> current
            }
        }
    }

    private fun setCloudVerificationResult(recordId: Long, message: String) {
        _cloudVerificationState.value = CloudVerificationState(recordId, isRunning = false, message = message)
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

}