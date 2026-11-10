package com.arbon.vata.floralens.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arbon.vata.floralens.data.objectbox.PlantRecord
import com.arbon.vata.floralens.data.objectbox.classifierLabel
import com.arbon.vata.floralens.data.objectbox.isDiagnosisUnknown
import com.arbon.vata.floralens.data.objectbox.isUserCorrected
import com.arbon.vata.floralens.domain.classifier.PlantLabels

@Composable
internal fun SupportedVerdictCard(record: PlantRecord, hasCloudResult: Boolean, needsCloud: Boolean) {
    val hasDisplayedDiagnosis = !hasCloudResult || record.hasHealthAssessment
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            if (hasCloudResult) {
                Text("Cloud result", style = MaterialTheme.typography.labelLarge)
            }
            Text(
                when {
                    record.isUserCorrected() -> "${record.commonName} (corrected)"
                    hasCloudResult -> record.commonName
                    else -> "Possible ${record.commonName}"
                },
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(4.dp))
            if (hasDisplayedDiagnosis) {
                DiagnosisChips(record)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    hasCloudResult && !record.hasHealthAssessment -> "Cloud health assessment unavailable"
                    record.isDiagnosisUnknown() -> "Diagnosis: Not sure"
                    record.isHealthy -> "Result: Healthy"
                    else -> "Result: ${record.diseaseName ?: "Unknown disease"}"
                },
                style = MaterialTheme.typography.titleMedium,
            )
            if (hasDisplayedDiagnosis && !record.isUserCorrected() && !record.isDiagnosisUnknown()) {
                LinearProgressIndicator(
                    progress = { record.confidence },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            if (!hasCloudResult && !record.isUserCorrected()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    if (needsCloud) {
                        "The on-device model is unsure. You can send this photo for a cloud second opinion."
                    } else {
                        "Unverified on-device estimate. Check the result against a clear photo before treating the plant."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun DiagnosisChips(record: PlantRecord) {
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
            AssistChip(onClick = {}, label = { Text("${(record.confidence * 100).toInt()}% confidence") })
        }
        if (record.isCloudVerified) AssistChip(onClick = {}, label = { Text("☁️ Cloud verified") })
    }
}

@Composable
internal fun DiagnosisCorrectionDialog(
    record: PlantRecord,
    onDismiss: () -> Unit,
    onCorrect: (String, String?) -> Unit,
) {
    val originalLabel = record.classifierLabel().takeIf { it in PlantLabels.CLASSES }
    val initialSpecies =
        record.detectedSpecies.takeIf { it in PlantLabels.SUPPORTED_SPECIES }
            ?: originalLabel?.let { PlantLabels.parse(it).species }
                ?.takeIf { it in PlantLabels.SUPPORTED_SPECIES }
            ?: PlantLabels.SUPPORTED_SPECIES.first()
    var selectedSpecies by remember(record.id) { mutableStateOf(initialSpecies) }
    var selectedLabel by remember(record.id) {
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

    AlertDialog(
        onDismissRequest = onDismiss,
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
                    onCorrect(selectedSpecies, selectedLabel.takeUnless { it == NOT_SURE_OPTION })
                    onDismiss()
                },
            ) { Text("Save correction") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
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
