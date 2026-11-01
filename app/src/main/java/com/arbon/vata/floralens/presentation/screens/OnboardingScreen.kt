package com.arbon.vata.floralens.presentation.screens

import com.arbon.vata.floralens.domain.classifier.PlantLabels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(
    onSeePlantsClick: () -> Unit = {},
    onStartScanClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🌿", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(12.dp))
        Text("Plant Doctor", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Your plants, diagnosed in 1 second. Offline.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.padding(16.dp)) {
                Text("Supports ${PlantLabels.SUPPORTED_SPECIES.size} plant groups", style = MaterialTheme.typography.titleSmall)
                Text("${PlantLabels.CLASSES.size} condition and healthy classes. No internet needed.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Text("🍌 Banana, 🥑 Avocado? Species ID yes, disease check in v2.0", style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onStartScanClick, modifier = Modifier.fillMaxWidth()) {
            Text("Start Scanning")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onSeePlantsClick, modifier = Modifier.fillMaxWidth()) {
            Text("See supported plants")
        }
    }
}

@Preview(showBackground = true, name = "Onboarding")
@Composable
fun OnboardingScreenPreview() {
    MaterialTheme { OnboardingScreen() }
}
