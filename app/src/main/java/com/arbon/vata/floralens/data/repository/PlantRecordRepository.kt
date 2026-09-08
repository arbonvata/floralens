package com.arbon.vata.floralens.data.repository

import android.graphics.Bitmap
import android.net.Uri
import com.arbon.vata.floralens.data.objectbox.PlantRecord
import com.arbon.vata.floralens.data.objectbox.PlantRecord_
import com.arbon.vata.floralens.data.photos.ScanPhotoStore
import io.objectbox.Box
import io.objectbox.kotlin.flow
import io.objectbox.query.QueryBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Single write path for scans: persists the photo via [ScanPhotoStore], stamps the
 * record with the photo path and scan time, then puts it into ObjectBox.
 */
class PlantRecordRepository(
    private val photoStore: ScanPhotoStore,
    private val plantRecordBox: Box<PlantRecord>,
) {
    /** Persists a new scan taken with the camera; always inserts a fresh record. */
    suspend fun saveScan(photo: Bitmap, record: PlantRecord): PlantRecord {
        val photoPath = withContext(Dispatchers.IO) { photoStore.save(photo) }
        return put(record.copy(id = 0), photoPath)
    }

    /** Persists a new scan imported from the gallery; inserts even if [record] reuses an id. */
    suspend fun saveScan(photoUri: Uri, record: PlantRecord): PlantRecord {
        val photoPath = withContext(Dispatchers.IO) { photoStore.save(photoUri) }
        return put(record.copy(id = 0), photoPath)
    }

    /** Decodes a gallery Uri into a bitmap for classification; null if unreadable. */
    suspend fun decode(photoUri: Uri): Bitmap? =
        withContext(Dispatchers.IO) { photoStore.decode(photoUri) }

    /** Decodes a previously saved scan photo, including its EXIF orientation. */
    suspend fun decodeSavedPhoto(relativePath: String): Bitmap? =
        withContext(Dispatchers.IO) {
            photoStore.decode(Uri.fromFile(photoStore.file(relativePath)))
        }

    /** Upsert for records without a new photo, e.g. "Notify me" wishlist entries or flag updates. */
    suspend fun save(record: PlantRecord): PlantRecord = put(record, photoPath = null)

    suspend fun getAll(): List<PlantRecord> =
        withContext(Dispatchers.IO) {
            plantRecordBox.query()
                .order(PlantRecord_.createdAt, QueryBuilder.DESCENDING)
                .build()
                .find()
        }

    /** Re-emits the newest-first list on every ObjectBox change, so new scans appear live. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeAll(): Flow<List<PlantRecord>> =
        plantRecordBox.query()
            .order(PlantRecord_.createdAt, QueryBuilder.DESCENDING)
            .build()
            .flow()

    /** Tracked scans, newest first — the raw feed for the My Plants timelines. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeTracked(): Flow<List<PlantRecord>> =
        plantRecordBox.query()
            .equal(PlantRecord_.isTracked, true)
            .order(PlantRecord_.createdAt, QueryBuilder.DESCENDING)
            .build()
            .flow()

    /** Flips the tracked flag on every scan of one species ("Stop tracking this plant"). */
    suspend fun setTracked(
        species: String,
        tracked: Boolean,
    ) = withContext(Dispatchers.IO) {
        val records =
            plantRecordBox.query()
                .equal(PlantRecord_.detectedSpecies, species, QueryBuilder.StringOrder.CASE_INSENSITIVE)
                .build()
                .find()
        records.forEach { it.isTracked = tracked }
        plantRecordBox.put(records)
    }

    private suspend fun put(record: PlantRecord, photoPath: String?): PlantRecord =
        withContext(Dispatchers.IO) {
            val toStore =
                if (photoPath != null) {
                    record.copy(photoPath = photoPath, createdAt = System.currentTimeMillis())
                } else {
                    record
                }
            toStore.id = plantRecordBox.put(toStore)
            toStore
        }
}