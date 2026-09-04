package com.arbon.vata.floralens.data.repository

import android.graphics.Bitmap
import android.net.Uri
import com.arbon.vata.floralens.data.objectbox.PlantRecord
import com.arbon.vata.floralens.data.photos.ScanPhotoStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.objectbox.Box
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PlantRecordRepositoryTest {

    private val photoStore = mockk<ScanPhotoStore>()
    private val plantRecordBox = mockk<Box<PlantRecord>>()
    private val repository = PlantRecordRepository(photoStore, plantRecordBox)

    private val putRecord = slot<PlantRecord>()

    /** Ids as they were at the moment put() was invoked, before the repository stamps the return value. */
    private val putIds = mutableListOf<Long>()

    @BeforeEach
    fun setUp() {
        every { plantRecordBox.put(capture(putRecord)) } answers {
            putIds.add(putRecord.captured.id)
            42L
        }
    }

    @Test
    fun `camera scan stores photo path, stamps timestamp and inserts as new record`() = runBlocking {
        val bitmap = mockk<Bitmap>()
        every { photoStore.save(bitmap) } returns "scan_photos/scan_123_00042.jpg"
        val input =
            PlantRecord(
                id = 7,
                detectedSpecies = "Tomato",
                diseaseName = "Early Blight",
                confidence = 0.82f,
                createdAt = 123L,
            )

        val saved = repository.saveScan(bitmap, input)

        assertEquals(0L, putIds.single())
        assertEquals(42L, saved.id)
        assertEquals("scan_photos/scan_123_00042.jpg", saved.photoPath)
        assertEquals("scan_photos/scan_123_00042.jpg", putRecord.captured.photoPath)
        assertTrue(putRecord.captured.createdAt > 123L)
        // The caller's record is left untouched: saveScan always inserts a copy.
        assertEquals(7L, input.id)
        assertNull(input.photoPath)
    }

    @Test
    fun `gallery scan with unreadable uri still saves a photo-less record`() = runBlocking {
        val uri = mockk<Uri>()
        every { photoStore.save(uri) } returns null
        val input = PlantRecord(id = 7, detectedSpecies = "Corn")

        val saved = repository.saveScan(uri, input)

        assertEquals(0L, putIds.single())
        assertEquals(42L, saved.id)
        assertNull(saved.photoPath)
    }

    @Test
    fun `save upserts without restamping existing record`() = runBlocking {
        val input =
            PlantRecord(
                id = 9,
                detectedSpecies = "Banana",
                isSupported = false,
                wantsNotifyV2 = true,
                photoPath = "scan_photos/keep.jpg",
                createdAt = 555L,
            )

        val saved = repository.save(input)

        assertEquals(9L, putIds.single())
        assertEquals("scan_photos/keep.jpg", putRecord.captured.photoPath)
        assertEquals(555L, putRecord.captured.createdAt)
        assertEquals(42L, saved.id)
    }
}