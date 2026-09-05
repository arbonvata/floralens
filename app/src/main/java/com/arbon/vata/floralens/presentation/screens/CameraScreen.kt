package com.arbon.vata.floralens.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

@Composable
fun CameraScreen(
    onPhotoCaptured: (Bitmap) -> Unit = {},
    onPhotoPicked: (Uri) -> Unit = {},
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasCameraPermission = granted
        }
    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let(onPhotoPicked)
        }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            CameraContent(
                onPhotoCaptured = onPhotoCaptured,
                onGalleryClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            )
        } else {
            Column(
                Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("📷", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Camera permission needed to diagnose your plants",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant camera access")
                }
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                ) {
                    Text("Or pick a photo instead")
                }
            }
        }
    }
}

@Composable
private fun CameraContent(
    onPhotoCaptured: (Bitmap) -> Unit,
    onGalleryClick: () -> Unit,
) {
    val context = LocalContext.current
    val controller =
        remember {
            LifecycleCameraController(context).apply {
                setEnabledUseCases(CameraController.IMAGE_CAPTURE)
                setImageCaptureFlashMode(ImageCapture.FLASH_MODE_OFF)
            }
        }
    var flashOn by remember { mutableStateOf(false) }
    var captureError by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply { this.controller = controller }
            },
        )

        // Leaf guide
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(220.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("🍃", style = MaterialTheme.typography.displayLarge)
            }
        }

        // Top pill
        Row(Modifier.align(Alignment.TopCenter).padding(top = 16.dp)) {
            AssistChip(onClick = {}, label = { Text("14 plants supported offline • 40ms") })
        }

        captureError?.let { error ->
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center).padding(16.dp),
            )
        }

        // Bottom controls
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(onClick = onGalleryClick) { Text("Gallery") }
            Button(
                onClick = {
                    controller.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bitmap = image.toRotatedBitmap()
                                image.close()
                                onPhotoCaptured(bitmap)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                captureError = exception.message ?: "Capture failed"
                            }
                        },
                    )
                },
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                    ),
            ) { Text("📸") }
            FilledTonalButton(
                onClick = {
                    flashOn = !flashOn
                    controller.setImageCaptureFlashMode(
                        if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF,
                    )
                },
            ) { Text(if (flashOn) "⚡ On" else "Flash") }
        }
    }
}

/** Decodes the JPEG buffer and applies the sensor rotation so downstream code sees an upright bitmap. */
private fun ImageProxy.toRotatedBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining()).also(buffer::get)
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val rotation = imageInfo.rotationDegrees
    if (rotation == 0) return decoded
    val matrix =
        Matrix().apply {
            postRotate(rotation.toFloat())
        }
    return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
}

@Preview(showBackground = true, name = "Camera")
@Composable
fun CameraScreenPreview() {
    MaterialTheme { CameraScreen() }
}