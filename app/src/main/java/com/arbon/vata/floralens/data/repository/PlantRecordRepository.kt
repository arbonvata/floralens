package com.arbon.vata.floralens.data.repository

import android.graphics.Bitmap
import android.net.Uri
import com.arbon.vata.floralens.data.objectbox.PlantRecord
import com.arbon.vata.floralens.data.objectbox.PlantRecord_
import com.arbon.vata.floralens.data.photos.ScanPhotoStore
import io.objectbox.Box
import io.objectbox.query.QueryBuilder
import kotlinx.coroutines.Dispatchers
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

    /** Upsert for records without a new photo, e.g. "Notify me" wishlist entries or flag updates. */
    suspend fun save(record: PlantRecord): PlantRecord = put(record, photoPath = null)

    suspend fun getAll(): List<PlantRecord> =
        withContext(Dispatchers.IO) {
            plantRecordBox.query()
                .order(PlantRecord_.createdAt, QueryBuilder.DESCENDING)
                .build()
                .find()
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