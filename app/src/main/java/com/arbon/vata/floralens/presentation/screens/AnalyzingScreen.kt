package com.arbon.vata.floralens.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

enum class AnalyzingStep { ON_DEVICE, SECOND_OPINION }

@Composable
fun AnalyzingScreen(
    step: AnalyzingStep = AnalyzingStep.ON_DEVICE,
    confidence: Float? = null,
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
                    AnalyzingStep.ON_DEVICE -> {
                        Text("Checking the photo on your device…")
                        LinearProgressIndicator( modifier = Modifier.fillMaxWidth().padding(top=8.dp))
                    }
                    AnalyzingStep.SECOND_OPINION -> {
                        confidence?.let { Text("On-device confidence: ${(it * 100).toInt()}%") }
                        Text("Getting a cloud second opinion…")
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top=8.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Please wait while we check your photo.", style = MaterialTheme.typography.labelSmall)
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