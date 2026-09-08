package com.arbon.vata.floralens.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.arbon.vata.floralens.data.objectbox.PlantRecord
import com.arbon.vata.floralens.data.objectbox.isUserCorrected
import com.arbon.vata.floralens.presentation.testdata.historyPreviewRecords
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class HistoryFilter(val label: String) {
    ALL("All"),
    SUPPORTED("Supported"),
    WISHLIST("Wishlist"),
    VERIFIED("Verified"),
}

private fun PlantRecord.matches(filter: HistoryFilter): Boolean =
    when (filter) {
        HistoryFilter.ALL -> true
        HistoryFilter.SUPPORTED -> isSupported
        HistoryFilter.WISHLIST -> !isSupported
        HistoryFilter.VERIFIED -> hasHealthAssessment
    }

/** Low-confidence results get the yellow flag even when classified healthy. */
private fun PlantRecord.healthBadge(): String = when {
    isUserCorrected() -> "✏️"
    !isHealthy -> "🔴"
    confidence < 0.80f -> "🟡"
    else -> "🟢"
}

@Composable
fun HistoryScreen(
    records: List<PlantRecord> = historyPreviewRecords,
    resolvePhoto: (PlantRecord) -> Any? = { null },
    onItemClick: (PlantRecord) -> Unit = {},
) {
    var filter by remember { mutableStateOf(HistoryFilter.ALL) }
    val filtered = records.filter { it.matches(filter) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        Text("History", style = MaterialTheme.typography.headlineSmall)
        Text(
            "${records.size} scans saved in ObjectBox",
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HistoryFilter.entries.forEach { f ->
                FilterChip(
                    onClick = { filter = f },
                    label = {
                        Text(
                            when (f) {
                                HistoryFilter.WISHLIST ->
                                    "Wishlist (${records.count { !it.isSupported }})"
                                else -> f.label
                            },
                        )
                    },
                    selected = filter == f,
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌱", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("No scans yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Diagnose a plant and it will show up here",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        } else {
            val dayFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
            val byDay = filtered.groupBy { dayFormat.format(Date(it.createdAt)) }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                byDay.forEach { (day, dayRecords) ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            day,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    items(dayRecords, key = { it.id }) { record ->
                        HistoryGridItem(
                            record = record,
                            photo = resolvePhoto(record),
                            onClick = { onItemClick(record) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryGridItem(
    record: PlantRecord,
    photo: Any?,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick),
    ) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (photo != null) {
                AsyncImage(
                    model = photo,
                    contentDescription = record.detectedSpecies,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    "🌿",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            Row(
                modifier =
                    Modifier.align(Alignment.BottomStart).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (record.isSupported) {
                    Text(record.healthBadge())
                    if (record.hasHealthAssessment) Text("☁️")
                }
            }
            if (!record.isSupported) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Text(
                        "v2.0",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            record.commonName.ifEmpty { record.detectedSpecies },
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            if (record.isUserCorrected()) "Corrected" else "${(record.confidence * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Preview(showBackground = true, name = "History")
@Composable
fun HistoryScreenPreview() {
    MaterialTheme { HistoryScreen() }
}