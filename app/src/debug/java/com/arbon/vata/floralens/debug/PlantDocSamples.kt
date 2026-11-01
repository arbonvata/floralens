package com.arbon.vata.floralens.debug

import android.content.Context
import android.net.Uri
import com.arbon.vata.floralens.data.objectbox.PlantRecord
import com.arbon.vata.floralens.data.repository.PlantRecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

/** Fixture labels are dataset expectations, never classifier predictions. */
object PlantDocSamples {
    private fun samples(context: Context) = JSONArray(
        context.assets.open("plantdoc/samples.json").bufferedReader().use { it.readText() },
    )

    private fun name(species: String, file: String) = "$species (PlantDoc sample: $file)"

    suspend fun import(context: Context, repository: PlantRecordRepository): Int = withContext(Dispatchers.IO) {
        val samples = samples(context)
        var inserted = 0
        for (index in 0 until samples.length()) {
            val sample = samples.getJSONObject(index)
            val species = sample.getString("species")
            val label = sample.getString("label")
            val name = name(species, sample.getString("file"))
            if (repository.getAll().any { it.commonName == name }) continue
            val temporary = File.createTempFile("plantdoc_", ".jpg", context.cacheDir)
            try {
                context.assets.open("plantdoc/${sample.getString("file")}").use { input ->
                    temporary.outputStream().use { input.copyTo(it) }
                }
                val disease = label.substringAfter("___")
                val record = repository.saveScan(
                    Uri.fromFile(temporary),
                    PlantRecord(
                        detectedSpecies = species,
                        commonName = name,
                        diseaseName = disease.takeUnless { it == "healthy" }?.replace('_', ' '),
                        isHealthy = disease == "healthy",
                        onDeviceLabel = label,
                        confidence = 0f,
                    ),
                )
                check(record.photoPath != null) { "Could not save ${sample.getString("file")}" }
                inserted++
            } finally {
                temporary.delete()
            }
        }
        inserted
    }

    suspend fun remove(context: Context, repository: PlantRecordRepository): Int = withContext(Dispatchers.IO) {
        val samples = samples(context)
        val names = (0 until samples.length()).map { index ->
            val sample = samples.getJSONObject(index)
            name(sample.getString("species"), sample.getString("file"))
        }.toSet()
        val records = repository.getAll().filter { it.commonName in names }
        records.forEach { repository.delete(it.id) }
        records.size
    }
}
