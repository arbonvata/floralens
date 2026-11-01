package com.arbon.vata.floralens.presentation.screens

import com.arbon.vata.floralens.domain.classifier.PlantLabels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class SupportedPlant(val emoji: String, val name: String, val info: String)

private val plants = PlantLabels.SUPPORTED_SPECIES.map { species ->
    val labels = PlantLabels.CLASSES.map(PlantLabels::parse).filter { it.species == species }
    val conditions = labels.count { !it.isHealthy }
    val info = if (conditions == 0) "Healthy class only" else "$conditions condition labels"
    SupportedPlant("🌿", species, info + if (conditions > 0 && labels.any { it.isHealthy }) " + healthy" else "")
}

@Composable
fun SupportedPlantsScreen(
    onGotItClick: () -> Unit = {},
    onNotifyBanana: () -> Unit = {},
    onNotifyAvocado: () -> Unit = {},
    bananaRequested: Boolean = false,
    avocadoRequested: Boolean = false,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("${PlantLabels.SUPPORTED_SPECIES.size} supported plant groups", style = MaterialTheme.typography.headlineSmall)
        Text("${PlantLabels.CLASSES.size} condition and healthy classes • Offline AI", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(plants.size) { i ->
                val p = plants[i]
                Card {
                    Column(Modifier.padding(12.dp)) {
                        Text("${p.emoji} ${p.name}", style = MaterialTheme.typography.titleSmall)
                        Text(p.info, style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(4.dp))

                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Coming in v2.0 - Vote with scans:", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(
                onClick = onNotifyBanana,
                modifier = Modifier.weight(1f),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("🍌 Banana")
                    Text(
                        if (bananaRequested) "Added" else "Notify Me",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Card(
                onClick = onNotifyAvocado,
                modifier = Modifier.weight(1f),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("🥑 Avocado")
                    Text(
                        if (avocadoRequested) "Added" else "Notify Me",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onGotItClick, modifier = Modifier.fillMaxWidth()) { Text("Got it, start scanning") }
    }
}

@Preview(showBackground = true, name = "Supported Plants")
@Composable
fun SupportedPlantsScreenPreview() {
    MaterialTheme { SupportedPlantsScreen() }
}
