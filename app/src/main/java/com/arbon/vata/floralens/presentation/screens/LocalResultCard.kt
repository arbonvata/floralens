package com.arbon.vata.floralens.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arbon.vata.floralens.data.objectbox.PlantRecord
import com.arbon.vata.floralens.data.objectbox.UNKNOWN_DIAGNOSIS_LABEL_PREFIX
import com.arbon.vata.floralens.domain.classifier.PlantLabels

@Composable
internal fun LocalResultCard(record: PlantRecord) {
    val label = record.originalModelLabel
        ?: record.onDeviceLabel.removePrefix(UNKNOWN_DIAGNOSIS_LABEL_PREFIX)
    val result = PlantLabels.parse(label)
    if (result.species.isBlank() || (!result.isHealthy && result.disease == null) || ':' in label) return

    Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("On-device result", style = MaterialTheme.typography.labelLarge)
            Text(result.species, style = MaterialTheme.typography.titleMedium)
            Text(if (result.isHealthy) "Healthy" else result.disease.orEmpty())
            record.originalModelConfidence?.let { confidence ->
                Text("${(confidence * 100).toInt()}% confidence", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
