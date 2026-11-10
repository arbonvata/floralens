package com.arbon.vata.floralens.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arbon.vata.floralens.presentation.viewmodel.CloudVerificationStep

@Composable
fun AnalyzingScreen() {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Analyzing photo", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                AnalysisProgress("Checking the leaf on your device…")
            }
        }
    }
}

@Composable
internal fun CloudAnalysisProgress(step: CloudVerificationStep) {
    AnalysisProgress(
        when (step) {
            CloudVerificationStep.PREPARING_PHOTO -> "Preparing the photo…"
            CloudVerificationStep.CHECKING_SPECIES -> "Checking species with Pl@ntNet…"
            CloudVerificationStep.CHECKING_HEALTH -> "Checking leaf health with Plant.id…"
        },
    )
}

@Composable
private fun AnalysisProgress(label: String) {
    Text(label, style = MaterialTheme.typography.bodyMedium)
    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp))
}

@Preview(showBackground = true)
@Composable
fun AnalyzingOnDevicePreview() {
    MaterialTheme { AnalyzingScreen() }
}
