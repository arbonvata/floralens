package com.arbon.vata.floralens.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arbon.vata.floralens.data.repository.PlantRecordRepository
import com.arbon.vata.floralens.domain.classifier.PlantClassifier
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun DebugSettingsScreen(onBack: () -> Unit, onHistory: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val repository = koinInject<PlantRecordRepository>()
    val classifier = koinInject<PlantClassifier>()
    val scope = rememberCoroutineScope()
    var results by remember { mutableStateOf<List<PlantDocEvaluation.Result>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    fun runOperation(remove: Boolean) {
        busy = true
        status = ""
        scope.launch {
            try {
                status = if (remove) {
                    "Removed ${PlantDocSamples.remove(context, repository)} sample records."
                } else {
                    "Imported ${PlantDocSamples.import(context, repository)} samples. Existing samples were skipped."
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                status = "Could not finish: ${error.message ?: "unknown error"}. You can retry."
            } finally {
                busy = false
            }
        }
    }

    fun evaluate() {
        busy = true
        status = "Running on-device AI on 25 photos…"
        results = emptyList()
        scope.launch {
            try {
                results = PlantDocEvaluation.run(context, classifier)
                status = "${results.count { it.matches }} / ${results.size} labels matched; " +
                    "${results.count { it.error != null }} errors."
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                status = "Could not run samples: ${error.message ?: "unknown error"}. You can retry."
            } finally {
                busy = false
            }
        }
    }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onBack, enabled = !busy) { Text("Back") }
        Text("Debug settings", style = MaterialTheme.typography.headlineMedium)
        Text("PlantDoc samples", style = MaterialTheme.typography.titleLarge)
        Text("Add 25 tomato and potato photos to History. Samples use dataset labels and 0% confidence; they are not app predictions. No cloud requests are made.")
        Button(onClick = { runOperation(false) }, enabled = !busy) { Text("Import PlantDoc samples") }
        OutlinedButton(onClick = { runOperation(true) }, enabled = !busy) { Text("Remove PlantDoc samples") }
        Button(onClick = { evaluate() }, enabled = !busy) { Text("Run on-device AI on samples") }
        Text("AI results below show actual model scores compared with dataset labels. Running this check does not change History records or call cloud services.")
        if (busy) Text("Working…")
        if (status.isNotEmpty()) Text(status)
        results.forEach { result ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(result.file, style = MaterialTheme.typography.titleSmall)
                Text("Expected: ${result.expectedLabel}")
                val prediction = result.prediction
                if (prediction != null) {
                    Text("Predicted: ${prediction.label}")
                    Text("Model confidence: ${String.format(Locale.US, "%.1f%%", prediction.confidence * 100f)}")
                    Text(if (result.matches) "Matches dataset label" else "Does not match dataset label")
                }
                result.error?.let { Text("Error: $it") }
            }
        }
        TextButton(onClick = onHistory, enabled = !busy) { Text("Open History") }
        Text("PlantDoc (2020), Singh et al. · CC BY 4.0. Source: github.com/pratikkayal/PlantDoc-Dataset. Images unchanged.")
    }
}
