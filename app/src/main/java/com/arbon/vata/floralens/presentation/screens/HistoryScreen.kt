package com.arbon.vata.floralens.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arbon.vata.floralens.data.objectbox.PlantRecord


@Composable
fun HistoryScreen(
    records: List<PlantRecord> = listOf(
        PlantRecord(detectedSpecies="Tomato", commonName="Tomato", diseaseName="Early_blight", confidence=0.82f, isSupported=true, isCloudVerified=true),
        PlantRecord(detectedSpecies="Apple", commonName="Apple", diseaseName="healthy", isHealthy=true, confidence=0.96f, isSupported=true),
        PlantRecord(detectedSpecies="Banana", commonName="Banana", isSupported=false),
    ),
    onFilterAll: () -> Unit = {},
    onFilterSupported: () -> Unit = {},
    onFilterWishlist: () -> Unit = {},
    onItemClick: (PlantRecord) -> Unit = {}
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("History", style = MaterialTheme.typography.headlineSmall)
        Text("${records.size} scans saved in ObjectBox", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(onClick = onFilterAll, label = {Text("All")}, selected = true)
            FilterChip(onClick = onFilterSupported, label = {Text("Supported")}, selected = false)
            FilterChip(onClick = onFilterWishlist, label = {Text("Wishlist (${records.count{!it.isSupported}})")}, selected = false)
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(records.size) { i ->
                val r = records[i]
                Card(onClick = {onItemClick(r)}, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("${r.commonName.ifEmpty{r.detectedSpecies}} - ${r.diseaseName ?: "ID only"}", style = MaterialTheme.typography.titleSmall)
                            Text("${(r.confidence*100).toInt()}% • ${if(r.isSupported) "Supported" else "Wishlist"}${if(r.isCloudVerified) " • Cloud verified" else ""}", style = MaterialTheme.typography.labelSmall)
                        }
                        Text(if(r.isHealthy) "🌱" else "🚨")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "History")
@Composable
fun HistoryScreenPreview() {
    MaterialTheme { HistoryScreen() }
}