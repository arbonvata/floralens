package com.arbon.vata.floralens.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.arbon.vata.floralens.data.objectbox.PlantRecord

sealed class ScanState {
    object Idle : ScanState()                          // Nothing happening
    object Scanning : ScanState()                      // TFLite is running
    data class OnDeviceResult(                         // TFLite finished
        val record: PlantRecord,
        val needsCloud: Boolean                        // true if confidence <0.80
    ) : ScanState()
    data class CloudResult(                            // Pl@ntNet/Gemini second opinion done
        val record: PlantRecord
    ) : ScanState()
    data class Unsupported(                            // Banana / Avocado detected
        val species: String
    ) : ScanState()
}

class PlantViewModel: ViewModel() {

}