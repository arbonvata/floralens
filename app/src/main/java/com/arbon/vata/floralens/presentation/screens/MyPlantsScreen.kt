package com.arbon.vata.floralens.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arbon.vata.floralens.data.objectbox.PlantRecord
import com.arbon.vata.floralens.presentation.testdata.trackedPreviewRecords
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * My Plants timeline. v1 groups a species' tracked scans into one profile; the
 * spec's ObjectBox vector-search grouping (similar leaves across species) waits
 * for the embedding field that ships with a feature-extraction model.
 */
@Composable
fun MyPlantsScreen(
    trackedRecords: List<PlantRecord> = trackedPreviewRecords,
    onUntrackClick: (String) -> Unit = {},
) {
    if (trackedRecords.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🪴", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(8.dp))
                Text("No tracked plants yet", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Tap \"Track plant\" on a scan result to follow its progress",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        return
    }

    val dayFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
    // Flow arrives newest-first; groupBy keeps species in most-recently-scanned
    // order, and each profile's timeline is reversed back to chronological.
    val plants = trackedRecords.groupBy { it.detectedSpecies }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        plants.forEach { (species, records) ->
            item(key = "plant_$species") {
                PlantProfileCard(
                    species = species,
                    timeline = records.sortedBy { it.createdAt },
                    dayFormat = dayFormat,
                    onUntrackClick = onUntrackClick,
                )
            }
        }
    }
}

@Composable
private fun PlantProfileCard(
    species: String,
    timeline: List<PlantRecord>,
    dayFormat: SimpleDateFormat,
    onUntrackClick: (String) -> Unit,
) {
    val latest = timeline.last()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("My $species", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${timeline.size} scans tracked",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Text(if (latest.isHealthy) "🟢" else "🔴")
            }
            Spacer(Modifier.height(12.dp))

            Text("Timeline", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            timeline.forEach { record ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(dayFormat.format(Date(record.createdAt)), style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${verdictLabel(record)} • ${(record.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                HorizontalDivider()
            }

            Spacer(Modifier.height(8.dp))
            Text("Latest scan confidence", style = MaterialTheme.typography.labelSmall)
            LinearProgressIndicator(
                progress = { latest.confidence },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
            Text(
                if (latest.isHealthy) "Looking healthy" else "Needs attention",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            TextButton(onClick = { onUntrackClick(species) }) {
                Text("Stop tracking")
            }
        }
    }
}

private fun verdictLabel(record: PlantRecord): String =
    when {
        !record.isSupported -> "Species ID only"
        record.isHealthy -> "Healthy"
        else -> record.diseaseName ?: "Diagnosis pending"
    }

@Preview(showBackground = true, name = "My Plants Timeline")
@Composable
fun MyPlantsPreview() {
    MaterialTheme { MyPlantsScreen() }
}
