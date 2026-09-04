package com.arbon.vata.floralens.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

enum class AnalyzingStep { CLEANING, ON_DEVICE, SECOND_OPINION }

@Composable
fun AnalyzingScreen(
    step: AnalyzingStep = AnalyzingStep.ON_DEVICE,
    topPrediction: String = "Tomato___Early_blight 82%",
    bitmapThumbDesc: String = "leaf thumb"
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(24.dp))
        Text("Analyzing...", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                when(step) {
                    AnalyzingStep.CLEANING -> {
                        Text("Step 1/3: Cleaning image... 224x224")
                        LinearProgressIndicator(progress = {0.2f}, modifier = Modifier.fillMaxWidth().padding(top=8.dp))
                    }
                    AnalyzingStep.ON_DEVICE -> {
                        Text("Step 2/3: On-device AI checking...")
                        Text(topPrediction, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        LinearProgressIndicator(progress = {0.6f}, modifier = Modifier.fillMaxWidth().padding(top=8.dp))
                    }
                    AnalyzingStep.SECOND_OPINION -> {
                        Text("Step 3/3: Confidence 58% - Getting second opinion from Pl@ntNet (free)...")
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top=8.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("This usually takes 1 second. ObjectBox + LiteRT, no cloud needed.", style = MaterialTheme.typography.labelSmall)
    }
}

@Preview(showBackground = true, name = "Analyzing - On Device")
@Composable
fun AnalyzingOnDevicePreview() {
    MaterialTheme { AnalyzingScreen(step = AnalyzingStep.ON_DEVICE) }
}

@Preview(showBackground = true, name = "Analyzing - Second Opinion")
@Composable
fun AnalyzingSecondOpinionPreview() {
    MaterialTheme { AnalyzingScreen(step = AnalyzingStep.SECOND_OPINION) }
}