package com.arbon.vata.floralens.data.repository

import android.graphics.Bitmap
import android.net.Uri
import com.arbon.vata.floralens.data.objectbox.PlantRecord
import com.arbon.vata.floralens.data.objectbox.PlantRecord_
import com.arbon.vata.floralens.data.photos.ScanPhotoStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.objectbox.Box
import io.objectbox.query.Query
import io.objectbox.query.QueryBuilder
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
    fun `delete removes persisted record and photo`() = runBlocking {
        val photo = mockk<java.io.File>()
        every { plantRecordBox.get(9L) } returns PlantRecord(id = 9, photoPath = "scan_photos/remove.jpg")
        every { plantRecordBox.remove(9L) } returns true
        every { photoStore.file("scan_photos/remove.jpg") } returns photo
        every { photo.delete() } returns true

        repository.delete(9L)

        verify(exactly = 1) { plantRecordBox.remove(9L) }
        verify(exactly = 1) { photo.delete() }
    }

    @Test
    fun `delete missing record is harmless`() = runBlocking {
        every { plantRecordBox.get(9L) } returns null

        repository.delete(9L)

        verify(exactly = 0) { plantRecordBox.remove(any<Long>()) }
        verify(exactly = 0) { photoStore.file(any()) }
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

    @Test
    fun `wishlist toggle creates a standalone request when none exists`() = runBlocking {
        stubWishlistQuery("Banana", emptyList())

        val requested = repository.toggleWishlistRequest("Banana")

        assertTrue(requested)
        assertEquals("Banana", putRecord.captured.detectedSpecies)
        assertEquals("Banana", putRecord.captured.commonName)
        assertFalse(putRecord.captured.isSupported)
        assertTrue(putRecord.captured.wantsNotifyV2)
        assertNull(putRecord.captured.photoPath)
    }

    @Test
    fun `wishlist toggle removes standalone entries and clears matching scan flags`() = runBlocking {
        val standaloneRequest =
            PlantRecord(
                id = 11,
                detectedSpecies = "Avocado",
                isSupported = false,
                wantsNotifyV2 = true,
            )
        val scannedRequest =
            PlantRecord(
                id = 12,
                detectedSpecies = "Avocado",
                isSupported = false,
                wantsNotifyV2 = true,
                photoPath = "scan_photos/avocado.jpg",
            )
        stubWishlistQuery("Avocado", listOf(standaloneRequest, scannedRequest))
        every { plantRecordBox.remove(standaloneRequest.id) } returns true

        val requested = repository.toggleWishlistRequest("Avocado")

        assertFalse(requested)
        verify { plantRecordBox.remove(standaloneRequest.id) }
        assertEquals(scannedRequest.id, putRecord.captured.id)
        assertFalse(putRecord.captured.wantsNotifyV2)
    }

    private fun stubWishlistQuery(
        species: String,
        existingRequests: List<PlantRecord>,
    ) {
        val queryBuilder = mockk<QueryBuilder<PlantRecord>>()
        val query = mockk<Query<PlantRecord>>()
        every { plantRecordBox.query() } returns queryBuilder
        every {
            queryBuilder.equal(
                PlantRecord_.detectedSpecies,
                species,
                QueryBuilder.StringOrder.CASE_INSENSITIVE,
            )
        } returns queryBuilder
        every { queryBuilder.equal(PlantRecord_.isSupported, false) } returns queryBuilder
        every { queryBuilder.equal(PlantRecord_.wantsNotifyV2, true) } returns queryBuilder
        every { queryBuilder.build() } returns query
        every { query.find() } returns existingRequests
    }
}