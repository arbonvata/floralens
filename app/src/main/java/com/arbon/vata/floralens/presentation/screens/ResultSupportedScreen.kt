package com.arbon.vata.floralens.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arbon.vata.floralens.data.objectbox.PlantRecord
import com.arbon.vata.floralens.data.objectbox.classifierLabel
import com.arbon.vata.floralens.data.objectbox.isDiagnosisUnknown
import com.arbon.vata.floralens.domain.advice.DiseaseAdvice
import com.arbon.vata.floralens.domain.classifier.PlantLabels
import com.arbon.vata.floralens.presentation.testdata.tomatoEarlyBlight
import com.arbon.vata.floralens.presentation.testdata.tomatoEarlyBlightCloudVerified
import com.arbon.vata.floralens.presentation.viewmodel.CloudVerificationStep

@Composable
fun ResultSupportedScreen(
    record: PlantRecord = tomatoEarlyBlight,
    needsCloud: Boolean = false,
    cloudVerificationStep: CloudVerificationStep? = null,
    cloudVerificationMessage: String? = null,
    onVerifyWithCloudClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onTrackClick: () -> Unit = {},
    onCorrectClick: (String, String?) -> Unit = { _, _ -> },
) {
    var correctionDialogOpen by remember { mutableStateOf(false) }
    val hasCloudResult = record.cloudLabel != null || record.hasHealthAssessment
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (hasCloudResult) LocalResultCard(record)
        SupportedVerdictCard(record, hasCloudResult, needsCloud)
        Spacer(Modifier.height(16.dp))
        CloudVerificationControls(
            isCloudVerified = record.isCloudVerified,
            needsCloud = needsCloud,
            step = cloudVerificationStep,
            message = cloudVerificationMessage,
            onVerifyClick = onVerifyWithCloudClick,
        )
        DiagnosisAdvice(record)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onSaveClick, modifier = Modifier.fillMaxWidth()) { Text("View in History") }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onTrackClick, modifier = Modifier.weight(1f)) { Text("Track plant") }
            OutlinedButton(onClick = { correctionDialogOpen = true }, modifier = Modifier.weight(1f)) { Text("Correct me") }
        }
    }

    if (correctionDialogOpen) {
        DiagnosisCorrectionDialog(
            record = record,
            onDismiss = { correctionDialogOpen = false },
            onCorrect = onCorrectClick,
        )
    }
}

@Composable
private fun CloudVerificationControls(
    isCloudVerified: Boolean,
    needsCloud: Boolean,
    step: CloudVerificationStep?,
    message: String?,
    onVerifyClick: () -> Unit,
) {
    if (step != null) {
        CloudAnalysisProgress(step)
    } else if (!isCloudVerified) {
        OutlinedButton(
            onClick = onVerifyClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (needsCloud) "Get cloud second opinion" else "Verify with cloud")
        }
    }
    message?.let {
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun DiagnosisAdvice(record: PlantRecord) {
    Text("Next steps", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(8.dp))
    Card {
        Column(Modifier.padding(12.dp)) {
            if (record.isDiagnosisUnknown()) {
                Text("No disease advice is shown because the diagnosis is unknown.")
            } else if (record.isHealthy) {
                Text("No disease treatment is suggested for this healthy result. Keep monitoring your plant.")
            } else {
                // Cloud disease names (Plant.id free text) may miss the map;
                // the corrected classifier label is the fallback spelling.
                val advice =
                    DiseaseAdvice.forDisease(record.diseaseName)
                        ?: record.classifierLabel().takeIf { it in PlantLabels.CLASSES }?.let(DiseaseAdvice::forClassLabel)
                if (advice == null) {
                    Text("Treatment guidance is not available for this diagnosis yet. Confirm the diagnosis before choosing a treatment.")
                } else {
                    advice.forEach { Text("• $it") }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Result - Supported 82%")
@Composable
fun ResultSupportedPreview() {
    MaterialTheme { ResultSupportedScreen() }
}

@Preview(showBackground = true, name = "Result - With Second Opinion")
@Composable
fun ResultWithCloudPreview() {
    MaterialTheme { ResultSupportedScreen(record = tomatoEarlyBlightCloudVerified) }
}
