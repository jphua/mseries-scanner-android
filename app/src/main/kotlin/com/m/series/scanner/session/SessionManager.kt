package com.m.series.scanner.session

import android.content.Context
import com.m.series.scanner.ble.KeiserBikeData
import java.io.File

/**
 * Manages the lifecycle of a single workout session.
 *
 * - Call [startSession] to open the CSV file and start recording.
 * - Call [recordData] for every incoming [KeiserBikeData]; only real-time
 *   packets from the selected bike are written to disk.
 * - Call [endSession] to close the file and retrieve it for sharing.
 *
 * Thread safety: [recordData] delegates to [CsvExporter.writeRecord], which is
 * @Synchronized. [startSession] and [endSession] should be called from a single thread.
 */
class SessionManager(context: Context) {

    private val exporter = CsvExporter(context)

    private var selectedBikeId: Int = -1
    private var sessionFile: File? = null
    private var active: Boolean = false

    val isActive: Boolean get() = active

    /**
     * Opens the CSV file and marks the session as started.
     * @param bikeId Equipment ID shown on the bike console (0–200).
     * @return The [File] that data will be written to.
     */
    fun startSession(bikeId: Int): File {
        check(!active) { "Session already active. Call endSession() first." }
        selectedBikeId = bikeId
        val file = exporter.open()
        sessionFile = file
        active = true
        return file
    }

    /**
     * Writes [data] to CSV if it matches the selected bike and is a real-time packet.
     * No-op if no session is active.
     */
    fun recordData(data: KeiserBikeData) {
        if (!active) return
        if (data.equipmentId != selectedBikeId) return
        if (!data.isRealTime) return
        exporter.writeRecord(data)
    }

    /**
     * Flushes and closes the CSV file.
     * @return The completed CSV [File].
     */
    fun endSession(): File {
        check(active) { "No active session to end." }
        active = false
        return exporter.close()
    }
}
