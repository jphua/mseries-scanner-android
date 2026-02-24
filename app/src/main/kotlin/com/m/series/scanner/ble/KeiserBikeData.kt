package com.m.series.scanner.ble

/**
 * Parsed data from a single Keiser M3i BLE advertisement.
 *
 * All fields are decoded from the 17-byte manufacturer-specific data value
 * (the 2-byte company ID 0x0102 is already stripped by the Android BLE stack).
 *
 * Reference: https://dev.keiser.com/mseries/direct/
 */
data class KeiserBikeData(
    /** Unix timestamp (ms) when this advertisement was received. */
    val timestamp: Long,

    /** Bike number (0–200), set on the console. Primary identifier within a room. */
    val equipmentId: Int,

    /** Firmware version, e.g. "6.30" */
    val versionMajor: Int,
    val versionMinor: Int,

    /**
     * Data type byte. Used to distinguish real-time vs. review/interval data.
     * Real-time: dataType == 0 || dataType in 128..227
     */
    val dataType: Int,

    /** Cadence in RPM (raw value divided by 10) */
    val cadenceRpm: Float,

    /**
     * Heart rate from the bike's 5 kHz receiver in BPM (raw divided by 10).
     * 0.0 means no HR monitor detected by the bike.
     */
    val heartRateBpm: Float,

    /** Power output in watts. */
    val powerWatts: Int,

    /** Accumulated caloric burn estimate for this interval (kcal). */
    val caloricBurn: Int,

    /** Elapsed time for this interval. */
    val durationMinutes: Int,
    val durationSeconds: Int,

    /**
     * Distance for this interval (raw divided by 10).
     * Unit depends on [distanceIsKm]: true = kilometres, false = miles.
     */
    val distanceValue: Float,
    val distanceIsKm: Boolean,

    /**
     * Current gear (1–24). Value 24 is also shown when braking (console shows 88).
     * 0 means the bike firmware does not transmit gear (version < 6.21).
     */
    val gear: Int,

    /** True when dataType indicates a live/real-time reading. */
    val isRealTime: Boolean,
) {
    val firmwareVersion: String get() = "$versionMajor.$versionMinor"

    val durationFormatted: String get() = "%d:%02d".format(durationMinutes, durationSeconds)

    val distanceUnit: String get() = if (distanceIsKm) "km" else "mi"
}
