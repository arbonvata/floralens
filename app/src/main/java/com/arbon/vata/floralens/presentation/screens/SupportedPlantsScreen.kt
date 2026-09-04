package com.arbon.vata.floralens.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class SupportedPlant(val emoji: String, val name: String, val info: String)

private val plants = listOf(
    SupportedPlant("🍎","Apple","4 diseases"), SupportedPlant("🫐","Blueberry","healthy"),
    SupportedPlant("🍒","Cherry","1 disease"), SupportedPlant("🌽","Corn","3 diseases"),
    SupportedPlant("🍇","Grape","3 diseases"), SupportedPlant("🍊","Orange","HLB"),
    SupportedPlant("🍑","Peach","1 disease"), SupportedPlant("🫑","Bell Pepper","1 disease"),
    SupportedPlant("🥔","Potato","2 diseases"), SupportedPlant("🍓","Strawberry","1 disease"),
    SupportedPlant("🍅","Tomato","9 diseases"), SupportedPlant("🫘","Soybean","healthy"),
    SupportedPlant("🎃","Squash","Powdery"), SupportedPlant("🍓","Raspberry","healthy"),
)

@Composable
fun SupportedPlantsScreen(
    onGotItClick: () -> Unit = {},
    onNotifyBanana: () -> Unit = {},
    onNotifyAvocado: () -> Unit = {}
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("We diagnose these 14", style = MaterialTheme.typography.headlineSmall)
        Text("38 diseases covered • Offline TFLite", style = MaterialTheme.typography.labelMedium)
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
                        Row {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text("Disease + Healthy", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Coming in v2.0 - Vote with scans:", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(onClick = onNotifyBanana, modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text("🍌 Banana")
                    Text("Notify Me", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Card(onClick = onNotifyAvocado, modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text("🥑 Avocado")
                    Text("Notify Me", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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
