package com.arbon.vata.floralens.presentation.viewmodel

import android.graphics.Bitmap
import com.arbon.vata.floralens.data.objectbox.PlantRecord
import com.arbon.vata.floralens.data.repository.OnboardingRepository
import com.arbon.vata.floralens.data.repository.PlantRecordRepository
import com.arbon.vata.floralens.domain.classifier.CascadeClassifier
import com.arbon.vata.floralens.domain.classifier.ClassPrediction
import com.arbon.vata.floralens.domain.diagnosis.DiseaseAssessment
import com.arbon.vata.floralens.domain.diagnosis.DiseaseVerifier
import com.arbon.vata.floralens.domain.species.SpeciesMatch
import com.arbon.vata.floralens.domain.species.SpeciesIdentifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
    fun `unsupported second opinion skips species identification and preserves the record`() = runTest {
        val vm = createViewModel()
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
        coEvery { diseaseVerifier.assess(bitmap) } returns null

        vm.verifyUnsupportedWithCloud(unsupportedBananaScan())
        advanceUntilIdle()

        val saved = savedRecord.captured
        assertFalse(saved.hasHealthAssessment)
        assertFalse(saved.isCloudVerified)
        assertEquals("No cloud health assessment was returned. Check your connection, API key, and credits.", vm.cloudVerificationState.value?.message)
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
