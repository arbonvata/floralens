package com.arbon.vata.floralens.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun CameraScreen(
    onShutterClick: () -> Unit = {},
    onGalleryClick: () -> Unit = {},
    onFlashToggle: () -> Unit = {}
) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Placeholder for CameraX preview
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(220.dp).clip(MaterialTheme.shapes.large).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Text("🍃", style = MaterialTheme.typography.displayLarge)
                }
                Spacer(Modifier.height(16.dp))
                Text("Point at a single leaf, good light", color = Color.White)
            }
        }
        // Top pill
        Row(Modifier.align(Alignment.TopCenter).padding(top = 48.dp)) {
            AssistChip(onClick = {}, label = { Text("14 plants supported offline • 40ms") })
        }
        // Bottom controls
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(onClick = onGalleryClick) { Text("Gallery") }
            Button(
                onClick = onShutterClick,
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) { Text("📸") }
            FilledTonalButton(onClick = onFlashToggle) { Text("Flash") }
        }
    }
}

@Preview(showBackground = true, name = "Camera")
@Composable
fun CameraScreenPreview() {
    MaterialTheme { CameraScreen() }
}