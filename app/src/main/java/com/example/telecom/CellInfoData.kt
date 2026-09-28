package com.example.telecom

/**
 * Live Cellular Network Telemetry Data.
 */
data class CellInfoData(
    val siteId: String = "N/A",
    val sectorId: String = "N/A",
    val rawCellId: Long = 0,
    val tac: Int = 0,
    val pci: Int = 0,
    val earfcn: Int = 0,
    val bandName: String = "1800 MHz (B3)",
    val frequencyLabel: String = "1800 MHz",
    val technology: String = "LTE", // LTE, 5G NR, 3G, 2G
    val operatorName: String = "Cellular Network",
    val mccMnc: String = "000-00",
    val rsrp: Int = -95, // dBm
    val rsrq: Int = -12, // dB
    val rssi: Int = -70, // dBm
    val sinr: Int = 15,  // dB
    val signalQuality: SignalQuality = SignalQuality.GOOD,
    val timestampMs: Long = System.currentTimeMillis()
)

enum class SignalQuality(val label: String, val colorHex: Long) {
    EXCELLENT("Excellent", 0xFF10B981), // Emerald
    GOOD("Good", 0xFF3B82F6),           // Blue
    FAIR("Fair", 0xFFF59E0B),           // Amber
    POOR("Poor", 0xFFEF4444)            // Red
}
