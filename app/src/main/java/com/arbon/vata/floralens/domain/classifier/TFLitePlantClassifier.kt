package com.arbon.vata.floralens.domain.classifier

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/** Plantulus Doctor EfficientNetV2-B0: 54 outputs with its own RGB 0–255 preprocessing.
 * Model provenance, source labels and input contract are bundled under assets/plantulus.
 */
class TFLitePlantClassifier(
    context: Context,
) : PlantClassifier {
    private val inferenceLock = Mutex()
    private val appContext = context.applicationContext

    private val interpreter: Interpreter by lazy {
        Interpreter(
            loadModelFile(),
            Interpreter
                .Options()
                .apply {
                    setNumThreads(Runtime.getRuntime().availableProcessors().coerceAtMost(4))
                },
        ).also { model ->
            check(model.getInputTensor(0).shape().contentEquals(intArrayOf(1, 224, 224, 3)))
            check(model.getInputTensor(0).dataType() == DataType.FLOAT32)
            check(model.getOutputTensor(0).shape().contentEquals(intArrayOf(1, PlantLabels.CLASSES.size)))
            check(model.getOutputTensor(0).dataType() == DataType.FLOAT32)
        }
    }

    override suspend fun classify(bitmap: Bitmap): List<ClassPrediction> =
        withContext(Dispatchers.Default) {
            val input = PlantulusPreprocessing.input(bitmap)
            inferenceLock.withLock {
                val output = Array(1) { FloatArray(PlantLabels.CLASSES.size) }
                interpreter.run(input, output)
                output[0]
                    .mapIndexed { index, confidence ->
                        ClassPrediction(PlantLabels.CLASSES[index], confidence)
                    }
                    .filter { it.confidence >= MIN_REPORTED_CONFIDENCE }
            }
        }

    private fun loadModelFile(): MappedByteBuffer {
        return appContext.assets.openFd(MODEL_FILE).use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).channel.use { channel ->
                channel.map(FileChannel.MapMode.READ_ONLY, descriptor.startOffset, descriptor.declaredLength)
            }
        }
    }

    private companion object {
        const val MODEL_FILE = "plant_disease_model.tflite"
        const val MIN_REPORTED_CONFIDENCE = 0.01f
    }
}