package com.arbon.vata.floralens.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arbon.vata.floralens.data.objectbox.PlantRecord
import com.arbon.vata.floralens.presentation.testdata.tomatoEarlyBlight


@Composable
fun ResultSupportedScreen(
    record: PlantRecord = tomatoEarlyBlight,
    needsCloud: Boolean = false,
    onSaveClick: () -> Unit = {},
    onTrackClick: () -> Unit = {},
    onCorrectClick: () -> Unit = {}
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("${record.commonName} detected", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(if(record.isHealthy) "Healthy" else "Sick") })
                    AssistChip(onClick = {}, label = { Text("${(record.confidence*100).toInt()}% confidence") })
                    if(needsCloud) AssistChip(onClick = {}, label = { Text("Cloud verified") })
                }
                Spacer(Modifier.height(8.dp))
                Text("Result: ${record.diseaseName}", style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(progress = {record.confidence}, modifier = Modifier.fillMaxWidth().padding(top=8.dp))
                if(needsCloud) {
                    Spacer(Modifier.height(8.dp))
                    Text("Updated via Pl@ntNet second opinion: Septoria more likely than Early Blight (spots circular, not concentric).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("What to do:", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Card { Column(Modifier.padding(12.dp)) {
            Text("• Remove affected leaves")
            Text("• Avoid overhead watering")
            Text("• Copper fungicide if spreads")
            Text("• Malmö weather: Don't water today, rain tomorrow")
        }}
        Spacer(Modifier.weight(1f))
        Button(onClick = onSaveClick, modifier = Modifier.fillMaxWidth()) { Text("Save to History (ObjectBox)") }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onTrackClick, modifier = Modifier.weight(1f)) { Text("Track plant") }
            OutlinedButton(onClick = onCorrectClick, modifier = Modifier.weight(1f)) { Text("Correct me") }
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
    MaterialTheme { ResultSupportedScreen(needsCloud = true) }
}
