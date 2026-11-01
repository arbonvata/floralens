package com.arbon.vata.floralens.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.arbon.vata.floralens.R
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
                Text(stringResource(R.string.unsupported_detected, species), style = MaterialTheme.typography.titleLarge)
                Text(
                    if (confidence != null) {
                        stringResource(R.string.unsupported_species_confidence, scientificName, (confidence * 100).toInt())
                    } else {
                        stringResource(R.string.unsupported_species, scientificName)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        if (hasCloudHealthAssessment) {
                            Text(stringResource(R.string.cloud_health_assessment))
                            Text(
                                if (healthAssessmentHealthy == true) {
                                    stringResource(R.string.cloud_healthy)
                                } else {
                                    stringResource(R.string.cloud_possible_issue, healthAssessmentDisease ?: stringResource(R.string.cloud_unknown_issue))
                                },
                            )
                        } else {
                            Text(stringResource(R.string.unsupported_no_offline))
                            Text(stringResource(R.string.unsupported_request_health), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Spacer(Modifier.weight(1f))
        Button(onClick = onNotifyClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.unsupported_notify, species))
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
                Text(stringResource(R.string.cloud_checking_plant_id))
            } else {
                Text(if (hasCloudHealthAssessment) stringResource(R.string.cloud_check_again) else stringResource(R.string.cloud_get_opinion))
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
