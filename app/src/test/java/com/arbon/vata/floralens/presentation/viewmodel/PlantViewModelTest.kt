package com.arbon.vata.floralens.presentation.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import com.arbon.vata.floralens.data.objectbox.PlantRecord
import com.arbon.vata.floralens.data.objectbox.isDiagnosisUnknown
import com.arbon.vata.floralens.data.repository.OnboardingRepository
import com.arbon.vata.floralens.data.repository.PlantRecordRepository
import com.arbon.vata.floralens.domain.classifier.CascadeClassifier
import com.arbon.vata.floralens.domain.classifier.ClassPrediction
import com.arbon.vata.floralens.domain.diagnosis.DiseaseAssessment
import com.arbon.vata.floralens.domain.diagnosis.DiseaseVerifier
import com.arbon.vata.floralens.domain.photos.PhotoRegion
import com.arbon.vata.floralens.domain.species.SpeciesIdentifier
import com.arbon.vata.floralens.domain.species.SpeciesMatch
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Cloud verification paths of [PlantViewModel]: the Plant.id health assessment
 * for unsupported species, the full species+disease recheck for supported ones,
 * and the unsupported-scan rewrite during the initial cascade.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlantViewModelTest {

    private val plantRecordRepository = mockk<PlantRecordRepository>()
    private val onboardingRepository = mockk<OnboardingRepository>()
    private val cascadeClassifier = mockk<CascadeClassifier>()
    private val speciesIdentifier = mockk<SpeciesIdentifier>()
    private val diseaseVerifier = mockk<DiseaseVerifier>()
    private val bitmap = mockk<Bitmap>()

    private val savedRecord = slot<PlantRecord>()

    @BeforeEach
    fun setUp() {
        every { onboardingRepository.isDone() } returns true
        every { plantRecordRepository.observeAll() } returns MutableStateFlow(emptyList())
        every { plantRecordRepository.observeTracked() } returns MutableStateFlow(emptyList())
        coEvery { plantRecordRepository.save(capture(savedRecord)) } answers { savedRecord.captured }
        coEvery { plantRecordRepository.saveScan(any<Bitmap>(), any()) } answers { secondArg<PlantRecord>().copy(id = 7L) }
        coEvery { plantRecordRepository.decodeSavedPhoto(any()) } returns bitmap
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** A Pl@ntNet-identified species outside the offline model, as saved by the scan cascade. */
    private fun unsupportedBananaScan() =
        PlantRecord(
            id = 5,
            detectedSpecies = "Banana",
            commonName = "Banana",
            diseaseName = "Late blight",
            isHealthy = false,
            confidence = 0.91f,
            onDeviceLabel = "user_corrected_unknown:Banana",
            cloudLabel = "Musa",
            isSupported = false,
            photoPath = "scan_photos/banana.jpg",
        )

    /**
     * The ViewModel's property initializers access viewModelScope, so Dispatchers.Main
     * must point at the test scheduler before construction — hence per-test creation.
     */
    private fun TestScope.createViewModel(): PlantViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return PlantViewModel(
            plantRecordRepository,
            onboardingRepository,
            cascadeClassifier,
            speciesIdentifier,
            diseaseVerifier,
        )
    }

    @Test
    fun `delete reports success after removing the record`() = runTest {
        val vm = createViewModel()
        coEvery { plantRecordRepository.delete(5L) } returns Unit

        assertTrue(vm.deleteRecord(unsupportedBananaScan()))

        coVerify(exactly = 1) { plantRecordRepository.delete(5L) }
    }

    @Test
    fun `delete reports a repository failure for retry`() = runTest {
        val vm = createViewModel()
        coEvery { plantRecordRepository.delete(5L) } throws IllegalStateException("Database unavailable")

        assertFalse(vm.deleteRecord(unsupportedBananaScan()))
    }

    @Test
    fun `unsupported second opinion skips species identification and preserves the record`() = runTest {
        val vm = createViewModel()
        coEvery { plantRecordRepository.get(5L) } returns unsupportedBananaScan()
        coEvery { diseaseVerifier.assess(bitmap) } returns DiseaseAssessment(isHealthy = false, diseaseName = "Black Sigatoka", confidence = 0.77f)

        vm.verifyUnsupportedWithCloud(unsupportedBananaScan())
        advanceUntilIdle()

        coVerify(exactly = 0) { speciesIdentifier.identify(any()) }
        val saved = savedRecord.captured
        assertTrue(saved.hasHealthAssessment)
        assertTrue(saved.isCloudVerified)
        assertFalse(saved.isHealthy)
        assertEquals("Black Sigatoka", saved.diseaseName)
        assertEquals("Musa", saved.cloudLabel)
        assertEquals(0.91f, saved.confidence)
        assertEquals("user_corrected_unknown:Banana", saved.onDeviceLabel)
        val state = vm.cloudVerificationState.value
        assertEquals(5L, state?.recordId)
        assertFalse(state?.isRunning == true)
        assertEquals("Cloud health assessment received.", state?.message)
    }

    @Test
    fun `unsupported second opinion with healthy verdict clears the stale on-device disease`() = runTest {
        val vm = createViewModel()
        coEvery { plantRecordRepository.get(5L) } returns unsupportedBananaScan()
        coEvery { diseaseVerifier.assess(bitmap) } returns DiseaseAssessment(isHealthy = true, diseaseName = null, confidence = 0.96f)

        vm.verifyUnsupportedWithCloud(unsupportedBananaScan())
        advanceUntilIdle()

        val saved = savedRecord.captured
        assertTrue(saved.isHealthy)
        assertNull(saved.diseaseName)
        assertEquals("Musa", saved.cloudLabel)
        assertTrue(saved.hasHealthAssessment)
    }

    @Test
    fun `unavailable assessment is reported without claiming verification`() = runTest {
        val vm = createViewModel()
        coEvery { plantRecordRepository.get(5L) } returns unsupportedBananaScan()
        coEvery { diseaseVerifier.assess(bitmap) } returns null

        vm.verifyUnsupportedWithCloud(unsupportedBananaScan())
        advanceUntilIdle()

        val saved = savedRecord.captured
        assertFalse(saved.hasHealthAssessment)
        assertFalse(saved.isCloudVerified)
        assertEquals("No cloud health assessment was returned. Check your connection, API key, and credits.", vm.cloudVerificationState.value?.message)
    }

    @Test
    fun `verification merges cloud results onto edits made while it ran`() = runTest {
        val vm = createViewModel()
        // The user tracked the plant while the network request was in flight;
        // the row re-read at response time carries that edit.
        coEvery { plantRecordRepository.get(5L) } returns unsupportedBananaScan().copy(isTracked = true)
        coEvery { diseaseVerifier.assess(bitmap) } returns DiseaseAssessment(isHealthy = false, diseaseName = "Black Sigatoka", confidence = 0.77f)

        vm.verifyUnsupportedWithCloud(unsupportedBananaScan())
        advanceUntilIdle()

        val saved = savedRecord.captured
        assertTrue(saved.isTracked)
        assertTrue(saved.isCloudVerified)
        assertEquals("Black Sigatoka", saved.diseaseName)
    }

    @Test
    fun `correction made during verification wins over the cloud response`() = runTest {
        val vm = createViewModel()
        val record = unsupportedBananaScan().copy(isSupported = true, detectedSpecies = "Tomato", commonName = "Tomato")
        val response = CompletableDeferred<DiseaseAssessment?>()
        coEvery { speciesIdentifier.identify(bitmap) } returns SpeciesMatch("Tomato", "Solanum lycopersicum", 0.9f, true)
        coEvery { diseaseVerifier.assess(bitmap) } coAnswers { response.await() }
        coEvery { plantRecordRepository.get(record.id) } answers { savedRecord.captured }

        vm.verifyWithCloud(record)
        runCurrent()
        vm.correctDiagnosis(record, "Corn", "Corn___Northern_Leaf_Blight")
        runCurrent()
        response.complete(DiseaseAssessment(true, null, 0.98f))
        advanceUntilIdle()

        coVerify(exactly = 1) { plantRecordRepository.save(any()) }
        assertEquals("Corn", savedRecord.captured.detectedSpecies)
        assertEquals("user_corrected:Corn___Northern_Leaf_Blight", savedRecord.captured.onDeviceLabel)
        assertFalse(savedRecord.captured.hasHealthAssessment)
        assertTrue(vm.cloudVerificationState.value?.message.orEmpty().contains("correction was kept"))
    }

    @Test
    fun `second opinion without a saved photo fails before any cloud call`() = runTest {
        val vm = createViewModel()

        vm.verifyUnsupportedWithCloud(unsupportedBananaScan().copy(photoPath = null))
        advanceUntilIdle()

        assertEquals("This scan has no saved photo to verify.", vm.cloudVerificationState.value?.message)
        coVerify(exactly = 0) { plantRecordRepository.save(any()) }
        coVerify(exactly = 0) { diseaseVerifier.assess(any()) }
    }

    @Test
    fun `supported verification applies the disease verdict and strips the user correction`() = runTest {
        val vm = createViewModel()
        val corrected =
            PlantRecord(
                id = 9,
                detectedSpecies = "Tomato",
                commonName = "Tomato",
                diseaseName = null,
                isHealthy = false,
                confidence = 0.62f,
                onDeviceLabel = "user_corrected:Tomato___Late_blight",
                photoPath = "scan_photos/tomato.jpg",
            )
        coEvery { speciesIdentifier.identify(bitmap) } returns SpeciesMatch("Tomato", "Lycopersicon esculentum", 0.88f, isSupported = true)
        coEvery { diseaseVerifier.assess(bitmap) } returns DiseaseAssessment(isHealthy = true, diseaseName = null, confidence = 0.98f)
        coEvery { plantRecordRepository.get(9L) } returns corrected

        vm.verifyWithCloud(corrected)
        advanceUntilIdle()

        val saved = savedRecord.captured
        assertEquals("Healthy", saved.cloudLabel)
        assertEquals("Tomato___Late_blight", saved.onDeviceLabel)
        assertTrue(saved.isHealthy)
        assertEquals(0.98f, saved.confidence)
        assertTrue(saved.hasHealthAssessment)
    }

    @Test
    fun `scan saves an unsupported cloud species without claiming a health assessment`() = runTest {
        val vm = createViewModel()
        coEvery { cascadeClassifier.diagnose(bitmap) } returns
            CascadeClassifier.Verdict.NeedsSecondOpinion(
                ClassPrediction("Tomato___healthy", 0.55f),
                ClassPrediction("Tomato___Late_blight", 0.44f),
            )
        coEvery { speciesIdentifier.identify(bitmap) } returns SpeciesMatch("Banana", "Musa", 0.92f, isSupported = false)

        vm.scan(bitmap)
        advanceUntilIdle()

        val saved = savedRecord.captured
        assertEquals("Banana", saved.commonName)
        assertEquals("Musa", saved.cloudLabel)
        assertFalse(saved.isSupported)
        assertFalse(saved.isCloudVerified)
        assertFalse(saved.hasHealthAssessment)
        val state = vm.scanState.value
        assertTrue(state is ScanState.Unsupported)
        assertEquals("Banana", (state as ScanState.Unsupported).species)
        assertEquals(saved, state.record)
        coVerify(exactly = 0) { diseaseVerifier.assess(any()) }
    }

    @Test
    fun `species change without health assessment clears the previous crop diagnosis`() = runTest {
        val vm = createViewModel()
        coEvery { cascadeClassifier.diagnose(bitmap) } returns CascadeClassifier.Verdict.NeedsSecondOpinion(
            ClassPrediction("Tomato___Late_blight", 0.7f), null,
        )
        coEvery { speciesIdentifier.identify(bitmap) } returns SpeciesMatch("Potato", "Solanum tuberosum", 0.9f, true)
        coEvery { diseaseVerifier.assess(bitmap) } returns null

        vm.scan(bitmap)
        advanceUntilIdle()

        val record = (vm.scanState.value as ScanState.CloudResult).record
        assertEquals("Potato", record.detectedSpecies)
        assertNull(record.diseaseName)
        assertTrue(record.isDiagnosisUnknown())
        assertFalse(record.isHealthy)
        assertFalse(record.hasHealthAssessment)
    }

    @Test
    fun `manual species change invalidates an old health assessment`() = runTest {
        val vm = createViewModel()
        val record = PlantRecord(id = 7, detectedSpecies = "Tomato", commonName = "Tomato",
            onDeviceLabel = "Tomato___Late_blight", diseaseName = "Late blight",
            isHealthy = false, isCloudVerified = true, hasHealthAssessment = true, photoPath = "scan.jpg")
        coEvery { plantRecordRepository.get(7L) } returns record
        coEvery { speciesIdentifier.identify(bitmap) } returns SpeciesMatch("Potato", "Solanum tuberosum", 0.9f, true)
        coEvery { diseaseVerifier.assess(bitmap) } returns null

        vm.verifyWithCloud(record)
        advanceUntilIdle()

        assertTrue(savedRecord.captured.isDiagnosisUnknown())
        assertNull(savedRecord.captured.diseaseName)
        assertFalse(savedRecord.captured.isCloudVerified)
        assertFalse(savedRecord.captured.hasHealthAssessment)
    }

    @Test
    fun `cloud diagnosis uses its own lower confidence and resolves unknown status`() = runTest {
        val vm = createViewModel()
        coEvery { cascadeClassifier.diagnose(bitmap) } returns CascadeClassifier.Verdict.NeedsSecondOpinion(
            ClassPrediction("Tomato___Late_blight", 0.79f), null,
        )
        coEvery { speciesIdentifier.identify(bitmap) } returns SpeciesMatch("Potato", "Solanum tuberosum", 0.9f, true)
        coEvery { diseaseVerifier.assess(bitmap) } returns DiseaseAssessment(true, null, 0.6f)

        vm.scan(bitmap)
        advanceUntilIdle()

        val record = (vm.scanState.value as ScanState.CloudResult).record
        assertTrue(record.isHealthy)
        assertFalse(record.isDiagnosisUnknown())
        assertEquals(0.6f, record.confidence)
        assertTrue(record.hasHealthAssessment)
    }

    @Test
    fun `same species cloud diagnosis never inherits the offline confidence`() = runTest {
        val vm = createViewModel()
        coEvery { cascadeClassifier.diagnose(bitmap) } returns CascadeClassifier.Verdict.NeedsSecondOpinion(
            ClassPrediction("Tomato___Late_blight", 0.79f), null,
        )
        coEvery { speciesIdentifier.identify(bitmap) } returns SpeciesMatch("Tomato", "Solanum lycopersicum", 0.9f, true)
        coEvery { diseaseVerifier.assess(bitmap) } returns DiseaseAssessment(true, null, 0.6f)
        vm.scan(bitmap)
        advanceUntilIdle()
        assertEquals(0.6f, (vm.scanState.value as ScanState.CloudResult).record.confidence)
    }

    @Test
    fun `camera focus crop goes only to the offline model while full photo is saved and uploaded`() = runTest {
        val vm = createViewModel()
        val crop = mockk<Bitmap>(relaxed = true)
        every { bitmap.width } returns 1000
        every { bitmap.height } returns 800
        mockkStatic(Bitmap::class)
        try {
            every { Bitmap.createBitmap(bitmap, 200, 100, 400, 400) } returns crop
            coEvery { cascadeClassifier.diagnose(crop) } returns CascadeClassifier.Verdict.NeedsSecondOpinion(
                ClassPrediction("Tomato___healthy", 0.6f), null,
            )
            coEvery { speciesIdentifier.identify(bitmap) } returns null
            coEvery { diseaseVerifier.assess(bitmap) } returns null

            vm.scan(bitmap, PhotoRegion(200, 100, 600, 500))
            advanceUntilIdle()

            coVerify(exactly = 1) { cascadeClassifier.diagnose(crop) }
            coVerify(exactly = 1) { plantRecordRepository.saveScan(bitmap, any()) }
            coVerify(exactly = 1) { speciesIdentifier.identify(bitmap) }
            coVerify(exactly = 1) { diseaseVerifier.assess(bitmap) }
            verify(exactly = 1) { crop.recycle() }
            verify(exactly = 0) { bitmap.recycle() }
        } finally {
            unmockkStatic(Bitmap::class)
        }
    }

    @Test
    fun `gallery import enters scanning before decoding so the previous result is not shown`() = runTest {
        val vm = createViewModel()
        val stateAtDecode = mutableListOf<ScanState>()
        coEvery { plantRecordRepository.decode(any<Uri>()) } answers {
            stateAtDecode.add(vm.scanState.value)
            null
        }
        coEvery { plantRecordRepository.saveScan(any<Uri>(), any()) } answers { secondArg<PlantRecord>().copy(id = 7L) }

        vm.scan(mockk<Uri>())
        advanceUntilIdle()

        assertEquals(listOf<ScanState>(ScanState.Scanning), stateAtDecode)
        assertTrue(vm.scanState.value is ScanState.Failed)
    }

    @Test
    fun `classification failure can be retried successfully`() = runTest {
        val vm = createViewModel()
        coEvery { cascadeClassifier.diagnose(bitmap) } throws IllegalStateException("Model failed")
        vm.scan(bitmap)
        advanceUntilIdle()
        assertTrue(vm.scanState.value is ScanState.Failed)

        coEvery { cascadeClassifier.diagnose(bitmap) } returns CascadeClassifier.Verdict.Confident(
            ClassPrediction("Tomato___healthy", 0.95f), null,
        )
        vm.retryFailedScan()
        assertTrue(vm.scanState.value is ScanState.Scanning)
        advanceUntilIdle()
        assertTrue(vm.scanState.value is ScanState.OnDeviceResult)
    }

    @Test
    fun `gallery decode exception becomes a retryable failure`() = runTest {
        val vm = createViewModel()
        coEvery { plantRecordRepository.decode(any<Uri>()) } throws SecurityException("Access denied")
        vm.scan(mockk<Uri>())
        advanceUntilIdle()
        assertTrue(vm.scanState.value is ScanState.Failed)
        coVerify(exactly = 0) { cascadeClassifier.diagnose(any()) }
    }

    @Test
    fun `photo save failure becomes a retryable failure`() = runTest {
        val vm = createViewModel()
        coEvery { cascadeClassifier.diagnose(bitmap) } returns CascadeClassifier.Verdict.Confident(
            ClassPrediction("Tomato___healthy", 0.95f), null,
        )
        coEvery { plantRecordRepository.saveScan(any<Bitmap>(), any()) } throws java.io.IOException("Disk full")
        vm.scan(bitmap)
        advanceUntilIdle()
        assertTrue(vm.scanState.value is ScanState.Failed)
    }

    @Test
    fun `scan cancellation is not presented as a failure`() = runTest {
        val vm = createViewModel()
        coEvery { cascadeClassifier.diagnose(bitmap) } throws kotlinx.coroutines.CancellationException()
        vm.scan(bitmap)
        advanceUntilIdle()
        assertFalse(vm.scanState.value is ScanState.Failed)
    }

    @Test
    fun `correct diagnosis rejects unsupported species without saving`() = runTest {
        val vm = createViewModel()

        vm.correctDiagnosis(unsupportedBananaScan(), correctedSpecies = "Banana", correctedLabel = null)
        advanceUntilIdle()

        coVerify(exactly = 0) { plantRecordRepository.save(any()) }
        assertTrue(vm.scanState.value is ScanState.Idle)
    }

    @Test
    fun `correct diagnosis rejects a label from a different species`() = runTest {
        val vm = createViewModel()

        vm.correctDiagnosis(unsupportedBananaScan(), correctedSpecies = "Corn", correctedLabel = "Tomato___Late_blight")
        advanceUntilIdle()

        coVerify(exactly = 0) { plantRecordRepository.save(any()) }
        assertTrue(vm.scanState.value is ScanState.Idle)
    }

    @Test
    fun `correct diagnosis saves a matching label with the correction prefix`() = runTest {
        val vm = createViewModel()

        vm.correctDiagnosis(unsupportedBananaScan(), correctedSpecies = "Tomato", correctedLabel = "Tomato___Late_blight")
        advanceUntilIdle()

        val saved = savedRecord.captured
        assertEquals("Tomato", saved.detectedSpecies)
        assertEquals("user_corrected:Tomato___Late_blight", saved.onDeviceLabel)
        assertEquals("Late blight", saved.diseaseName)
        assertFalse(saved.isHealthy)
        assertNull(saved.cloudLabel)
        assertFalse(saved.isCloudVerified)
        assertFalse(saved.hasHealthAssessment)
    }

    @Test
    fun `species-only correction is stored as an unknown diagnosis`() = runTest {
        val vm = createViewModel()

        vm.correctDiagnosis(unsupportedBananaScan(), correctedSpecies = "Apple", correctedLabel = null)
        advanceUntilIdle()

        val saved = savedRecord.captured
        assertEquals("user_corrected_unknown:Apple", saved.onDeviceLabel)
        assertNull(saved.diseaseName)
        assertFalse(saved.isHealthy)
    }

    @Test
    fun `correcting after a cloud result downgrades to a final on-device verdict`() = runTest {
        val vm = createViewModel()
        coEvery { cascadeClassifier.diagnose(bitmap) } returns
            CascadeClassifier.Verdict.NeedsSecondOpinion(
                ClassPrediction("Tomato___healthy", 0.55f),
                ClassPrediction("Tomato___Late_blight", 0.44f),
            )
        coEvery { speciesIdentifier.identify(bitmap) } returns SpeciesMatch("Tomato", "Lycopersicon esculentum", 0.88f, isSupported = true)
        coEvery { diseaseVerifier.assess(bitmap) } returns DiseaseAssessment(isHealthy = true, diseaseName = null, confidence = 0.9f)
        vm.scan(bitmap)
        advanceUntilIdle()
        assertTrue(vm.scanState.value is ScanState.CloudResult)

        val scanned = (vm.scanState.value as ScanState.CloudResult).record
        vm.correctDiagnosis(scanned, correctedSpecies = "Corn", correctedLabel = "Corn___Northern_Leaf_Blight")
        advanceUntilIdle()

        val state = vm.scanState.value as ScanState.OnDeviceResult
        assertFalse(state.needsCloud)
        assertEquals("user_corrected:Corn___Northern_Leaf_Blight", state.record.onDeviceLabel)
        assertEquals("Corn", state.record.detectedSpecies)
    }
}
