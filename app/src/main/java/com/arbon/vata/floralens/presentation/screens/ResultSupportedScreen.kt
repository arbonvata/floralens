package com.arbon.vata.floralens.presentation.screens

import androidx.compose.foundation.layout.*
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
import com.arbon.vata.floralens.data.objectbox.isUserCorrected
import com.arbon.vata.floralens.domain.advice.DiseaseAdvice
import com.arbon.vata.floralens.domain.classifier.PlantLabels
import com.arbon.vata.floralens.presentation.testdata.tomatoEarlyBlight
import com.arbon.vata.floralens.presentation.testdata.tomatoEarlyBlightCloudVerified


@Composable
fun ResultSupportedScreen(
    record: PlantRecord = tomatoEarlyBlight,
    needsCloud: Boolean = false,
    isCloudVerificationRunning: Boolean = false,
    cloudVerificationMessage: String? = null,
    onVerifyWithCloudClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onTrackClick: () -> Unit = {},
    onCorrectClick: (String, String?) -> Unit = { _, _ -> },
) {
    var correctionDialogOpen by remember { mutableStateOf(false) }
    val originalLabel = record.classifierLabel().takeIf { it in PlantLabels.CLASSES }
    val initialSpecies =
        record.detectedSpecies.takeIf { it in PlantLabels.SUPPORTED_SPECIES }
            ?: originalLabel?.let { PlantLabels.parse(it).species }
                ?.takeIf { it in PlantLabels.SUPPORTED_SPECIES }
            ?: PlantLabels.SUPPORTED_SPECIES.first()
    var selectedSpecies by remember(record.id, correctionDialogOpen) { mutableStateOf(initialSpecies) }
    var selectedLabel by remember(record.id, correctionDialogOpen) {
        mutableStateOf(
            when {
                record.isDiagnosisUnknown() -> NOT_SURE_OPTION
                originalLabel != null && PlantLabels.parse(originalLabel).species == initialSpecies -> originalLabel
                else ->
                    PlantLabels.CLASSES.firstOrNull {
                        val parsed = PlantLabels.parse(it)
                        parsed.species == initialSpecies && parsed.disease == record.diseaseName && parsed.isHealthy == record.isHealthy
                    } ?: NOT_SURE_OPTION
            },
        )
    }
    val diseaseOptions = listOf(NOT_SURE_OPTION) + labelsFor(selectedSpecies)

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    when {
                        record.isUserCorrected() -> "${record.commonName} (corrected)"
                        record.isCloudVerified -> "${record.commonName} confirmed"
                        else -> "Possible ${record.commonName}"
                    },
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                when {
                                    record.isDiagnosisUnknown() -> "Diagnosis unknown"
                                    record.isHealthy -> "Healthy"
                                    else -> "Sick"
                                },
                            )
                        },
                    )
                    if (record.isUserCorrected()) {
                        AssistChip(onClick = {}, label = { Text("Corrected by you") })
                    } else if (!record.isDiagnosisUnknown()) {
                        AssistChip(onClick = {}, label = { Text("${(record.confidence*100).toInt()}% confidence") })
                    }
                    if(record.isCloudVerified) AssistChip(onClick = {}, label = { Text("☁️ Cloud verified") })
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        record.isDiagnosisUnknown() -> "Diagnosis: Not sure"
                        record.isHealthy -> "Result: Healthy"
                        else -> "Result: ${record.diseaseName ?: "Unknown disease"}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                if (!record.isUserCorrected() && !record.isDiagnosisUnknown()) {
                    LinearProgressIndicator(
                        progress = { record.confidence },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
                if (!record.isCloudVerified && !record.isUserCorrected()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Unverified on-device estimate. Check the result against a clear photo before treating the plant.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                if(needsCloud) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (record.isCloudVerified && record.cloudLabel != null) {
                            "Cloud health assessment: ${record.cloudLabel}"
                        } else {
                            "Disease check unavailable - showing the unverified on-device estimate"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (!record.isCloudVerified) {
            OutlinedButton(
                onClick = onVerifyWithCloudClick,
                enabled = !isCloudVerificationRunning,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isCloudVerificationRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Checking with cloud…")
                } else {
                    Text("Verify with cloud")
                }
            }
        }
        cloudVerificationMessage?.let { message ->
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
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
                            ?: originalLabel?.let(DiseaseAdvice::forClassLabel)
                    if (advice == null) {
                        Text("Treatment guidance is not available for this diagnosis yet. Confirm the diagnosis before choosing a treatment.")
                    } else {
                        advice.forEach { Text("• $it") }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onSaveClick, modifier = Modifier.fillMaxWidth()) { Text("View in History") }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onTrackClick, modifier = Modifier.weight(1f)) { Text("Track plant") }
            OutlinedButton(onClick = { correctionDialogOpen = true }, modifier = Modifier.weight(1f)) { Text("Correct me") }
        }
    }

    if (correctionDialogOpen) {
        AlertDialog(
            onDismissRequest = { correctionDialogOpen = false },
            title = { Text("Correct this result") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Choose the plant and the diagnosis that best match your photo.")
                    CorrectionDropdown(
                        label = "Plant",
                        selected = selectedSpecies,
                        options = PlantLabels.SUPPORTED_SPECIES.sorted(),
                        onSelected = { species ->
                            selectedSpecies = species
                            selectedLabel = NOT_SURE_OPTION
                        },
                    ) { it }
                    CorrectionDropdown(
                        label = "Diagnosis",
                        selected = selectedLabel,
                        options = diseaseOptions,
                        onSelected = { selectedLabel = it },
                        display = ::diagnosisOptionName,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCorrectClick(selectedSpecies, selectedLabel.takeUnless { it == NOT_SURE_OPTION })
                        correctionDialogOpen = false
                    },
                ) { Text("Save correction") }
            },
            dismissButton = {
                TextButton(onClick = { correctionDialogOpen = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun CorrectionDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    display: (String) -> String = { it },
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(display(selected))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(display(option)) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

private fun labelsFor(species: String): List<String> =
    PlantLabels.CLASSES.filter { PlantLabels.parse(it).species == species }

private fun diagnosisName(label: String): String =
    PlantLabels.parse(label).disease ?: "Healthy"

private const val NOT_SURE_OPTION = "__not_sure__"

private fun diagnosisOptionName(label: String): String =
    if (label == NOT_SURE_OPTION) "Not sure" else diagnosisName(label)

@Preview(showBackground = true, name = "Result - Supported 82%")
@Composable
fun ResultSupportedPreview() {
    MaterialTheme { ResultSupportedScreen() }
}

@Preview(showBackground = true, name = "Result - With Second Opinion")
@Composable
fun ResultWithCloudPreview() {
    MaterialTheme { ResultSupportedScreen(record = tomatoEarlyBlightCloudVerified, needsCloud = true) }
}
