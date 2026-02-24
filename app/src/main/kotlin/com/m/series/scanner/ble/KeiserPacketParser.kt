package com.m.series.scanner.ble

/**
 * Parses the manufacturer-specific data value from a Keiser M-Series advertisement.
 *
 * The Android BLE stack strips the 2-byte company ID (0x02, 0x01) before delivering
 * the data to [android.bluetooth.le.ScanResult.scanRecord], so the byte array
 * received here starts at what the Keiser spec calls byte index 2.
 *
 * Expected layout (17 bytes, little-endian):
 * ```
 * [0]     version_major  (u8, decimal)
 * [1]     version_minor  (u8, decimal)
 * [2]     data_type      (u8)
 * [3]     equipment_id   (u8, 0–200)
 * [4–5]   cadence        (u16 LE, RPM × 10)
 * [6–7]   heart_rate     (u16 LE, BPM × 10; 0 = no HRM)
 * [8–9]   power          (u16 LE, watts)
 * [10–11] caloric_burn   (u16 LE, kcal)
 * [12]    duration_min   (u8)
 * [13]    duration_sec   (u8)
 * [14–15] distance       (u16 LE, × 10; MSB=1 → km, MSB=0 → miles)
 * [16]    gear           (u8, 1–24; 0 = not supported; 24 also means braking)
 * ```
 */
object KeiserPacketParser {

    private const val MIN_PACKET_SIZE = 17

    /**
     * Parse [data] into a [KeiserBikeData], or return null if the packet is
     * too short or otherwise invalid.
     */
    fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): KeiserBikeData? {
        if (data.size < MIN_PACKET_SIZE) return null

        val versionMajor = data[0].toInt() and 0xFF
        val versionMinor = data[1].toInt() and 0xFF
        val dataType     = data[2].toInt() and 0xFF
        val equipmentId  = data[3].toInt() and 0xFF

        val cadenceRaw   = readU16LE(data, 4)
        val heartRateRaw = readU16LE(data, 6)
        val powerRaw     = readU16LE(data, 8)
        val caloricBurn  = readU16LE(data, 10)

        val durationMin  = data[12].toInt() and 0xFF
        val durationSec  = data[13].toInt() and 0xFF

        val distanceRaw  = readU16LE(data, 14)
        val distanceIsKm = (distanceRaw and 0x8000) != 0
        val distanceVal  = (distanceRaw and 0x7FFF)

        val gear         = data[16].toInt() and 0xFF

        val isRealTime = dataType == 0 || dataType in 128..227

        return KeiserBikeData(
            timestamp        = timestamp,
            equipmentId      = equipmentId,
            versionMajor     = versionMajor,
            versionMinor     = versionMinor,
            dataType         = dataType,
            cadenceRpm       = cadenceRaw / 10f,
            heartRateBpm     = heartRateRaw / 10f,
            powerWatts       = powerRaw,
            caloricBurn      = caloricBurn,
            durationMinutes  = durationMin,
            durationSeconds  = durationSec,
            distanceValue    = distanceVal / 10f,
            distanceIsKm     = distanceIsKm,
            gear             = gear,
            isRealTime       = isRealTime,
        )
    }

    /** Read an unsigned 16-bit little-endian integer from [buf] at [offset]. */
    private fun readU16LE(buf: ByteArray, offset: Int): Int {
        val lo = buf[offset].toInt() and 0xFF
        val hi = buf[offset + 1].toInt() and 0xFF
        return (hi shl 8) or lo
    }
}
