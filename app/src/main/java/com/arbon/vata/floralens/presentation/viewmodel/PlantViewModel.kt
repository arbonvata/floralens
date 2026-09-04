package com.arbon.vata.floralens.presentation.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arbon.vata.floralens.data.objectbox.PlantRecord
import com.arbon.vata.floralens.data.repository.PlantRecordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    data class Saved(                                  // Scan persisted to ObjectBox
        val record: PlantRecord
    ) : ScanState()
}

class PlantViewModel(
    private val plantRecordRepository: PlantRecordRepository,
) : ViewModel() {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    fun saveScan(photo: Bitmap, record: PlantRecord) {
        viewModelScope.launch {
            val saved = plantRecordRepository.saveScan(photo, record)
            _scanState.update { ScanState.Saved(saved) }
        }
    }

    fun saveScan(photoUri: Uri, record: PlantRecord) {
        viewModelScope.launch {
            val saved = plantRecordRepository.saveScan(photoUri, record)
            _scanState.update { ScanState.Saved(saved) }
        }
    }
}
