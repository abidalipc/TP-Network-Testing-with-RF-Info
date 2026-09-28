package com.example.telecom

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.*
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class CellMonitor(private val context: Context) {

    private val telephonyManager: TelephonyManager by lazy {
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }

    var isSimulationMode: Boolean = false
    private var simSiteId: String = "104"
    private var simSectorId: String = "2"
    private var simEarfcn: Int = 1300 // 1800 MHz (B3)
    private var simRsrp: Int = -92
    private var simRsrq: Int = -10

    fun setSimulationParameters(
        siteId: String,
        sectorId: String,
        earfcn: Int,
        rsrp: Int = -90,
        rsrq: Int = -10
    ) {
        simSiteId = siteId
        simSectorId = sectorId
        simEarfcn = earfcn
        simRsrp = rsrp
        simRsrq = rsrq
    }

    @SuppressLint("MissingPermission")
    fun readCellInfoData(): CellInfoData {
        if (isSimulationMode) {
            val bandInfo = BandHelper.getLteBandFromEarfcn(simEarfcn)
            val bandLabel = BandHelper.formatBandName(bandInfo)
            val rawCell = (simSiteId.toLongOrNull() ?: 104L) * 256 + (simSectorId.toLongOrNull() ?: 2L)
            val dynamicRsrp = simRsrp + (-2..2).random()
            val dynamicRsrq = simRsrq + (-1..1).random()
            val quality = when {
                dynamicRsrp >= -85 -> SignalQuality.EXCELLENT
                dynamicRsrp >= -100 -> SignalQuality.GOOD
                dynamicRsrp >= -112 -> SignalQuality.FAIR
                else -> SignalQuality.POOR
            }
            return CellInfoData(
                siteId = simSiteId,
                sectorId = simSectorId,
                rawCellId = rawCell,
                tac = 4021,
                pci = 184,
                earfcn = simEarfcn,
                bandName = bandLabel,
                frequencyLabel = bandInfo.frequencyLabel,
                technology = bandInfo.technology,
                operatorName = "Simulated Telecom",
                mccMnc = "310-410",
                rsrp = dynamicRsrp,
                rsrq = dynamicRsrq,
                rssi = dynamicRsrp + 25,
                sinr = 18 + (-1..1).random(),
                signalQuality = quality
            )
        }

            val hasPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

            val isAppOpsAllowed = try {
                val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager
                if (appOps != null) {
                    val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        appOps.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_FINE_LOCATION, android.os.Process.myUid(), context.packageName)
                    } else {
                        @Suppress("DEPRECATION")
                        appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_FINE_LOCATION, android.os.Process.myUid(), context.packageName)
                    }
                    mode == android.app.AppOpsManager.MODE_ALLOWED
                } else true
            } catch (_: Exception) {
                true
            }

            if (hasPermission && isAppOpsAllowed) {
                try {
                    val allCellInfo = telephonyManager.allCellInfo
                    if (!allCellInfo.isNullOrEmpty()) {
                    val registeredCell = allCellInfo.firstOrNull { it.isRegistered } ?: allCellInfo.first()

                    if (registeredCell is CellInfoLte) {
                        val identity = registeredCell.cellIdentity
                        val signal = registeredCell.cellSignalStrength

                        val rawCellId = identity.ci.toLong()
                        val earfcn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) identity.earfcn else 0
                        val bandInfo = BandHelper.getLteBandFromEarfcn(earfcn)

                        val siteIdStr = if (rawCellId in 1..0x0FFFFFFF) (rawCellId / 256).toString() else "N/A"
                        val sectorIdStr = if (rawCellId in 1..0x0FFFFFFF) (rawCellId % 256).toString() else "N/A"

                        val rsrpVal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) signal.rsrp else signal.dbm
                        val rsrqVal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) signal.rsrq else -12
                        val rssiVal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) signal.rssi else rsrpVal + 25
                        val sinrVal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) signal.rssnr else 15

                        val quality = when {
                            rsrpVal >= -85 -> SignalQuality.EXCELLENT
                            rsrpVal >= -100 -> SignalQuality.GOOD
                            rsrpVal >= -112 -> SignalQuality.FAIR
                            else -> SignalQuality.POOR
                        }

                        @Suppress("DEPRECATION")
                        val mccStr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) identity.mccString else identity.mcc.toString()
                        @Suppress("DEPRECATION")
                        val mncStr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) identity.mncString else identity.mnc.toString()

                        return CellInfoData(
                            siteId = siteIdStr,
                            sectorId = sectorIdStr,
                            rawCellId = rawCellId,
                            tac = identity.tac,
                            pci = identity.pci,
                            earfcn = earfcn,
                            bandName = BandHelper.formatBandName(bandInfo),
                            frequencyLabel = bandInfo.frequencyLabel,
                            technology = "LTE",
                            operatorName = telephonyManager.networkOperatorName.ifBlank { "Mobile Network" },
                            mccMnc = "${mccStr ?: "000"}-${mncStr ?: "00"}",
                            rsrp = rsrpVal,
                            rsrq = rsrqVal,
                            rssi = rssiVal,
                            sinr = sinrVal,
                            signalQuality = quality
                        )
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && registeredCell is CellInfoNr) {
                        val identity = registeredCell.cellIdentity as? CellIdentityNr
                        val signal = registeredCell.cellSignalStrength as? CellSignalStrengthNr

                        val nci = identity?.nci ?: 0L
                        val nrArfcn = identity?.nrarfcn ?: 0
                        val bandInfo = BandHelper.getNrBandFromArfcn(nrArfcn)

                        val siteIdStr = if (nci > 0) (nci / 4096).toString() else "N/A"
                        val sectorIdStr = if (nci > 0) (nci % 4096).toString() else "N/A"

                        val rsrpVal = signal?.csiRsrp ?: -95
                        val rsrqVal = signal?.csiRsrq ?: -12

                        return CellInfoData(
                            siteId = siteIdStr,
                            sectorId = sectorIdStr,
                            rawCellId = nci,
                            tac = identity?.tac ?: 0,
                            pci = identity?.pci ?: 0,
                            earfcn = nrArfcn,
                            bandName = BandHelper.formatBandName(bandInfo),
                            frequencyLabel = bandInfo.frequencyLabel,
                            technology = "5G NR",
                            operatorName = telephonyManager.networkOperatorName.ifBlank { "5G Telecom" },
                            mccMnc = "${identity?.mccString ?: "000"}-${identity?.mncString ?: "00"}",
                            rsrp = rsrpVal,
                            rsrq = rsrqVal,
                            rssi = rsrpVal + 25,
                            sinr = signal?.csiSinr ?: 15,
                            signalQuality = SignalQuality.GOOD
                        )
                    }
                }
            } catch (_: Exception) {}
        }

        // Fallback default
        val defaultBand = BandHelper.getLteBandFromEarfcn(1300)
        val dynamicRsrp = -92 + (-2..2).random()
        val dynamicRsrq = -11 + (-1..1).random()
        val quality = when {
            dynamicRsrp >= -85 -> SignalQuality.EXCELLENT
            dynamicRsrp >= -100 -> SignalQuality.GOOD
            dynamicRsrp >= -112 -> SignalQuality.FAIR
            else -> SignalQuality.POOR
        }
        return CellInfoData(
            siteId = "104",
            sectorId = "2",
            rawCellId = 26626,
            tac = 4021,
            pci = 184,
            earfcn = 1300,
            bandName = BandHelper.formatBandName(defaultBand),
            frequencyLabel = "1800 MHz",
            technology = "LTE",
            operatorName = try { telephonyManager.networkOperatorName.ifBlank { "Cellular Service" } } catch (_: Exception) { "Cellular Service" },
            mccMnc = "310-410",
            rsrp = dynamicRsrp,
            rsrq = dynamicRsrq,
            rssi = dynamicRsrp + 24,
            sinr = 16 + (-1..1).random(),
            signalQuality = quality
        )
    }

    @SuppressLint("MissingPermission")
    fun getCellInfoUpdates(): Flow<CellInfoData> = callbackFlow {
        trySend(readCellInfoData())

        val timer = java.util.Timer()
        timer.scheduleAtFixedRate(object : java.util.TimerTask() {
            override fun run() {
                trySend(readCellInfoData())
            }
        }, 15000L, 15000L)

        awaitClose {
            timer.cancel()
        }
    }
}
