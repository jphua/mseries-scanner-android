package com.m.series.scanner.ui

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.m.series.scanner.ble.BleScanner
import com.m.series.scanner.ble.KeiserBikeData
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for [MainActivity]. Scans for Keiser bikes and exposes the discovered
 * set as a [StateFlow] of equipment ID → latest [KeiserBikeData].
 */
@SuppressLint("MissingPermission")
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = BleScanner(application)

    /** Map of equipment ID → most recent advertisement from each distinct bike. */
    private val _bikes = MutableStateFlow<Map<Int, KeiserBikeData>>(emptyMap())
    val bikes: StateFlow<Map<Int, KeiserBikeData>> = _bikes

    sealed class ScanState {
        object Idle : ScanState()
        object Scanning : ScanState()
        data class Error(val message: String) : ScanState()
    }

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState

    private var scanJob: Job? = null

    /** Returns true if Bluetooth is enabled on this device. */
    fun isBluetoothEnabled(): Boolean {
        val bm = getApplication<Application>()
            .getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bm.adapter?.isEnabled == true
    }

    fun startScan() {
        if (scanJob?.isActive == true) return
        _bikes.value = emptyMap()
        _scanState.value = ScanState.Scanning

        scanJob = viewModelScope.launch {
            scanner.scanResults()
                .catch { e ->
                    _scanState.value = ScanState.Error(e.message ?: "Unknown error")
                }
                .collect { result ->
                    val current = _bikes.value.toMutableMap()
                    current[result.data.equipmentId] = result.data
                    _bikes.value = current
                }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _scanState.value = ScanState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }
}
