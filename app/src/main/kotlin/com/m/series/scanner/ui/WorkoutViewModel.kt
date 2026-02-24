package com.m.series.scanner.ui

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.m.series.scanner.ble.BleScanner
import com.m.series.scanner.ble.KeiserBikeData
import com.m.series.scanner.session.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for [WorkoutActivity]. Receives live bike data and writes each
 * real-time packet to the CSV via [SessionManager].
 */
@SuppressLint("MissingPermission")
class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = BleScanner(application)
    private val sessionManager = SessionManager(application)

    private val _latestData = MutableStateFlow<KeiserBikeData?>(null)
    val latestData: StateFlow<KeiserBikeData?> = _latestData

    sealed class WorkoutState {
        object Idle : WorkoutState()
        object Active : WorkoutState()
        data class Finished(val csvFile: File) : WorkoutState()
        data class Error(val message: String) : WorkoutState()
    }

    private val _state = MutableStateFlow<WorkoutState>(WorkoutState.Idle)
    val state: StateFlow<WorkoutState> = _state

    private var scanJob: Job? = null
    private var sessionFile: File? = null

    /** Start session for [bikeId] and begin collecting advertisement data. */
    fun startSession(bikeId: Int) {
        if (_state.value is WorkoutState.Active) return

        sessionFile = sessionManager.startSession(bikeId)
        _state.value = WorkoutState.Active

        scanJob = viewModelScope.launch {
            scanner.scanResults()
                .catch { e -> _state.value = WorkoutState.Error(e.message ?: "BLE error") }
                .collect { result ->
                    sessionManager.recordData(result.data)
                    // Only update UI display for the selected bike
                    if (result.data.equipmentId == bikeId && result.data.isRealTime) {
                        _latestData.value = result.data
                    }
                }
        }
    }

    /** Stop collecting data, close the CSV, and transition to [WorkoutState.Finished]. */
    fun endSession() {
        scanJob?.cancel()
        scanJob = null
        val file = sessionManager.endSession()
        _state.value = WorkoutState.Finished(file)
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        if (sessionManager.isActive) sessionManager.endSession()
    }
}
