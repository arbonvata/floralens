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
    confidence: Float? = 0.94f,
    hasCloudHealthAssessment: Boolean = false,
    healthAssessmentHealthy: Boolean? = null,
    healthAssessmentDisease: String? = null,
    isSecondOpinionRunning: Boolean = false,
    secondOpinionMessage: String? = null,
    onNotifyClick: () -> Unit = {},
    onSecondOpinionClick: () -> Unit = {}
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
            Column(Modifier.padding(16.dp)) {
                Text("$species detected", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (confidence != null) {
                        "Species ID: $scientificName ✓ ${(confidence * 100).toInt()}% (Pl@ntNet)"
                    } else {
                        "Species ID: $scientificName (Pl@ntNet)"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        if (hasCloudHealthAssessment) {
                            Text("Cloud health assessment")
                            Text(
                                if (healthAssessmentHealthy == true) {
                                    "Plant.id found no health issue."
                                } else {
                                    "Possible issue: ${healthAssessmentDisease ?: "health concern detected"}"
                                },
                            )
                        } else {
                            Text("No offline disease diagnosis is available for this plant.")
                            Text("Request an on-demand cloud health assessment.", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Spacer(Modifier.weight(1f))
        Button(onClick = onNotifyClick, modifier = Modifier.fillMaxWidth()) {
            Text("Notify me when $species is supported")
        }
        Spacer(Modifier.height(8.dp))
        secondOpinionMessage?.let { message ->
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        TextButton(
            onClick = onSecondOpinionClick,
            enabled = !isSecondOpinionRunning,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isSecondOpinionRunning) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Checking with Plant.id…")
            } else {
                Text(if (hasCloudHealthAssessment) "Check again with Plant.id" else "Get AI second opinion")
            }
        }
    }
}

@Preview(showBackground = true, name = "Result - Unsupported Banana")
@Composable
fun ResultUnsupportedBananaPreview() {
}

@Preview(showBackground = true, name = "Result - Unsupported Avocado")
@Composable
fun ResultUnsupportedAvocadoPreview() {
}
