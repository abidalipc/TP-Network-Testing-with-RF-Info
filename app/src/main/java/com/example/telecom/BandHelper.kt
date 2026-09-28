package com.example.telecom

/**
 * BandHelper converts EARFCN (LTE), ARFCN (GSM/UMTS), or NR-ARFCN (5G) values
 * to human-readable Band names, Frequencies (MHz), and standard telecom designations.
 */
object BandHelper {

    data class BandInfo(
        val bandNumber: String,
        val frequencyLabel: String, // e.g. "1800 MHz"
        val duplex: String, // FDD or TDD
        val technology: String // LTE, 5G NR, 3G, 2G
    )

    fun getLteBandFromEarfcn(earfcn: Int): BandInfo {
        return when (earfcn) {
            in 0..599 -> BandInfo("Band 1", "2100 MHz", "FDD", "LTE")
            in 600..1199 -> BandInfo("Band 2", "1900 MHz", "FDD", "LTE")
            in 1200..1949 -> BandInfo("Band 3", "1800 MHz", "FDD", "LTE")
            in 1950..2399 -> BandInfo("Band 4", "1700/2100 MHz (AWS)", "FDD", "LTE")
            in 2400..2649 -> BandInfo("Band 5", "850 MHz", "FDD", "LTE")
            in 2750..3449 -> BandInfo("Band 7", "2600 MHz", "FDD", "LTE")
            in 3450..3799 -> BandInfo("Band 8", "900 MHz", "FDD", "LTE")
            in 6150..6449 -> BandInfo("Band 20", "800 MHz", "FDD", "LTE")
            in 9210..9659 -> BandInfo("Band 28", "700 MHz", "FDD", "LTE")
            in 37750..38249 -> BandInfo("Band 38", "2600 MHz", "TDD", "LTE")
            in 38650..39649 -> BandInfo("Band 40", "2300 MHz", "TDD", "LTE")
            in 40240..41239 -> BandInfo("Band 41", "2500 MHz", "TDD", "LTE")
            else -> {
                if (earfcn > 0) BandInfo("LTE EARFCN $earfcn", "Custom Band", "FDD", "LTE")
                else BandInfo("Unknown Band", "-- MHz", "FDD", "LTE")
            }
        }
    }

    fun getNrBandFromArfcn(nrArfcn: Int): BandInfo {
        return when (nrArfcn) {
            in 422000..434000 -> BandInfo("n1", "2100 MHz", "FDD", "5G NR")
            in 361000..376000 -> BandInfo("n3", "1800 MHz", "FDD", "5G NR")
            in 524000..538000 -> BandInfo("n7", "2600 MHz", "FDD", "5G NR")
            in 185000..192000 -> BandInfo("n8", "900 MHz", "FDD", "5G NR")
            in 158200..164200 -> BandInfo("n20", "800 MHz", "FDD", "5G NR")
            in 140600..150600 -> BandInfo("n28", "700 MHz", "FDD", "5G NR")
            in 499200..537999 -> BandInfo("n41", "2500 MHz", "TDD", "5G NR")
            in 620000..653333 -> BandInfo("n78", "3500 MHz", "TDD", "5G NR")
            else -> {
                if (nrArfcn > 0) BandInfo("5G NR $nrArfcn", "NR Band", "TDD/FDD", "5G NR")
                else BandInfo("Unknown 5G Band", "-- MHz", "NR", "5G NR")
            }
        }
    }

    fun formatBandName(bandInfo: BandInfo): String {
        return "${bandInfo.bandNumber} (${bandInfo.frequencyLabel})"
    }
}
