package com.arbon.vata.floralens.domain.classifier

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * On-device classifier running the PlantVillage MobileNetV2 model bundled as
 * plant_disease_model.tflite (from the MIT-licensed obeshor/Plant-Diseases-Detector
 * project; 38 classes, output index i = PlantLabels.CLASSES[i]).
 *
 * Preprocessing mirrors the reference app: scale to 224x224, feed RGB floats
 * in [0,1] — the model normalizes internally.
 */
class TFLitePlantClassifier(
    context: Context,
) : PlantClassifier {
    private val appContext = context.applicationContext

    private val interpreter: Interpreter by lazy {
        Interpreter(
            loadModelFile(),
            Interpreter
                .Options()
                .apply {
                    setNumThreads(Runtime.getRuntime().availableProcessors().coerceAtMost(4))
                },
        )
    }

    override suspend fun classify(bitmap: Bitmap): List<ClassPrediction> =
        withContext(Dispatchers.Default) {
            val input = bitmapToInputBuffer(bitmap)
            val output = Array(1) { FloatArray(PlantLabels.CLASSES.size) }
            interpreter.run(input, output)
            output[0]
                .mapIndexed { index, confidence ->
                    ClassPrediction(PlantLabels.CLASSES[index], confidence)
                }
                .filter { it.confidence >= MIN_REPORTED_CONFIDENCE }
        }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = appContext.assets.openFd(MODEL_FILE)
        FileInputStream(fileDescriptor.fileDescriptor).channel.use { channel ->
            return channel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength,
            )
        }
    }

    private fun bitmapToInputBuffer(bitmap: Bitmap): ByteBuffer {
        val scaled =
            if (bitmap.width == INPUT_SIZE && bitmap.height == INPUT_SIZE) {
                bitmap
            } else {
                Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
            }
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE)
        byteBuffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        pixels.forEach { pixel ->
            byteBuffer.putFloat(((pixel shr 16) and 0xFF) / 255f) // R
            byteBuffer.putFloat(((pixel shr 8) and 0xFF) / 255f) // G
            byteBuffer.putFloat((pixel and 0xFF) / 255f) // B
        }
        return byteBuffer
    }

    private companion object {
        const val MODEL_FILE = "plant_disease_model.tflite"
        const val INPUT_SIZE = 224
        const val PIXEL_SIZE = 3
        const val MIN_REPORTED_CONFIDENCE = 0.01f
    }
}