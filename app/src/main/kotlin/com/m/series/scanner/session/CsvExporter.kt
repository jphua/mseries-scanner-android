package com.m.series.scanner.session

import android.content.Context
import com.m.series.scanner.ble.KeiserBikeData
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Streams session rows directly to a CSV file as they arrive, keeping memory usage flat.
 *
 * Lifecycle:
 * 1. [open] — creates the file and writes the header row.
 * 2. [writeRecord] — appends one row and flushes. Safe to call on any thread.
 * 3. [close] — flushes and closes the writer; returns the [File].
 */
class CsvExporter(private val context: Context) {

    private var writer: BufferedWriter? = null
    private var file: File? = null

    /** Open a new session file. Must be called before [writeRecord]. */
    fun open(): File {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dir = context.getExternalFilesDir(null)
            ?: context.filesDir  // fallback to internal storage
        dir.mkdirs()

        val f = File(dir, "mseries_session_$ts.csv")
        val bw = BufferedWriter(FileWriter(f, /* append= */ false))

        bw.write(
            "timestamp,equipment_id,cadence_rpm,heart_rate_bpm,power_watts," +
            "caloric_burn_kcal,duration_min,duration_sec," +
            "distance,distance_unit,gear,data_type\n"
        )
        bw.flush()

        writer = bw
        file = f
        return f
    }

    /**
     * Write one row. The timestamp is formatted as ISO-8601 for readability.
     * @throws IllegalStateException if [open] was not called first.
     */
    @Synchronized
    fun writeRecord(data: KeiserBikeData) {
        val bw = writer ?: error("CsvExporter is not open. Call open() first.")
        val ts = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)
            .format(Date(data.timestamp))

        bw.write(
            "$ts,${data.equipmentId},${data.cadenceRpm},${data.heartRateBpm}," +
            "${data.powerWatts},${data.caloricBurn}," +
            "${data.durationMinutes},${data.durationSeconds}," +
            "${data.distanceValue},${data.distanceUnit}," +
            "${data.gear},${data.dataType}\n"
        )
        bw.flush()
    }

    /** Flush and close the file. Returns the completed [File]. */
    @Synchronized
    fun close(): File {
        val f = file ?: error("CsvExporter was never opened.")
        writer?.flush()
        writer?.close()
        writer = null
        return f
    }
}
