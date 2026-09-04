package com.arbon.vata.floralens.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun MyPlantsScreen(
    plantNickname: String = "My Tomato #1",
    timeline: List<Pair<String,String>> = listOf(
        "Aug 1" to "Healthy 95%",
        "Aug 10" to "Early Blight 82%",
        "Aug 15" to "Recovering 70%"
    )
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(plantNickname, style = MaterialTheme.typography.headlineSmall)
        Text("Tracked via ObjectBox vector search - similar leaves grouped", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Timeline", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                timeline.forEach { (date, status) ->
                    Row(Modifier.fillMaxWidth().padding(vertical=4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(date, style = MaterialTheme.typography.labelMedium)
                        Text(status, style = MaterialTheme.typography.bodySmall)
                    }
                    Divider()
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Health Progress", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = {0.7f}, modifier = Modifier.fillMaxWidth())
                Text("70% healthier than Aug 10", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top=4.dp))
            }
        }
    }
}

@Preview(showBackground = true, name = "My Plants Timeline")
@Composable
fun MyPlantsPreview() {
    MaterialTheme { MyPlantsScreen() }
}
