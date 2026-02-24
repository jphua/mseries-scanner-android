package com.m.series.scanner.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Keiser M-Series BLE constants.
 * Company ID 0x0102 is transmitted little-endian on air; Android's BluetoothLeScanner
 * uses it as a big-endian int in the manufacturer data filter.
 */
object KeiserBle {
    const val COMPANY_ID = 0x0102
    const val DEVICE_NAME = "M3"
}

/**
 * Wraps Android's [android.bluetooth.le.BluetoothLeScanner] to emit [KeiserScanResult]
 * for every Keiser M-Series advertisement received.
 *
 * Usage:
 * ```
 * BleScanner(context).scanResults().collect { result -> ... }
 * ```
 *
 * The Flow is active while collected and cancels the underlying BLE scan on cancellation.
 */
class BleScanner(private val context: Context) {

    data class KeiserScanResult(
        /** BLE MAC address of the advertising device */
        val address: String,
        val data: KeiserBikeData,
    )

    @SuppressLint("MissingPermission")
    fun scanResults(): Flow<KeiserScanResult> = callbackFlow {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE)
                as BluetoothManager
        val scanner = bluetoothManager.adapter?.bluetoothLeScanner
            ?: throw IllegalStateException("Bluetooth is not available or not enabled")

        val filter = ScanFilter.Builder()
            .setManufacturerData(KeiserBle.COMPANY_ID, null)
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setReportDelay(0)
            .build()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val manufacturerData = result.scanRecord
                    ?.getManufacturerSpecificData(KeiserBle.COMPANY_ID)
                    ?: return

                val parsed = KeiserPacketParser.parse(manufacturerData) ?: return

                // Extra guard: device local name should be "M3"
                val name = result.scanRecord?.deviceName ?: ""
                if (name.isNotEmpty() && !name.startsWith("M3")) return

                trySend(KeiserScanResult(address = result.device.address, data = parsed))
            }

            override fun onScanFailed(errorCode: Int) {
                close(IllegalStateException("BLE scan failed with error code $errorCode"))
            }
        }

        scanner.startScan(listOf(filter), settings, callback)

        awaitClose {
            scanner.stopScan(callback)
        }
    }
}
