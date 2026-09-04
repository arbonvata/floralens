package com.arbon.vata.floralens.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ResultUnsupportedScreen(
    species: String = "Banana",
    scientificName: String = "Musa",
    confidence: Float = 0.94f,
    queuePosition: Int = 128,
    onNotifyClick: () -> Unit = {},
    onCareTipsClick: () -> Unit = {},
    onSecondOpinionClick: () -> Unit = {}
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
            Column(Modifier.padding(16.dp)) {
                Text("$species detected", style = MaterialTheme.typography.titleLarge)
                Text("Species ID: $scientificName ✓ ${(confidence*100).toInt()}% (Pl@ntNet free)", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Disease check: Coming in v2.0")
                        Text("You're #$queuePosition to request $species. This helps us prioritize.", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Still useful in v1.0:", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("• Sun: Bright indirect")
                Text("• Water: Keep moist, not wet")
                Text("• General care from Trefle.io (free API)")
            }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onNotifyClick, modifier = Modifier.fillMaxWidth()) {
            Text("Notify me when $species is supported")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onCareTipsClick, modifier = Modifier.fillMaxWidth()) { Text("Get general care tips") }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSecondOpinionClick, modifier = Modifier.fillMaxWidth()) { Text("Get AI second opinion (1 cloud credit)") }
    }
}

@Preview(showBackground = true, name = "Result - Unsupported Banana")
@Composable
fun ResultUnsupportedBananaPreview() {
    MaterialTheme { ResultUnsupportedScreen(species = "Banana", scientificName = "Musa", queuePosition = 128) }
}

@Preview(showBackground = true, name = "Result - Unsupported Avocado")
@Composable
fun ResultUnsupportedAvocadoPreview() {
    MaterialTheme { ResultUnsupportedScreen(species = "Avocado", scientificName = "Persea americana", queuePosition = 42) }
}