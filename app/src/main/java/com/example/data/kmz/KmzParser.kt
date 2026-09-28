package com.example.data.kmz

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.db.SectorEntity
import com.example.data.db.SiteEntity
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.StringReader
import java.util.zip.ZipInputStream

data class ParsedKmzResult(
    val sites: List<SiteEntity>,
    val sectors: List<SectorEntity>,
    val totalPlacemarks: Int
)

class KmzParser(private val context: Context) {

    fun parseKmzOrKmlUri(uri: Uri): ParsedKmzResult {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return ParsedKmzResult(emptyList(), emptyList(), 0)
        return try {
            val fileName = uri.path?.lowercase() ?: ""
            if (fileName.endsWith(".kmz") || isZipStream(inputStream)) {
                // Reset stream and parse zip
                val zipStream = context.contentResolver.openInputStream(uri)?.let { ZipInputStream(it) }
                var kmlContent = ""
                if (zipStream != null) {
                    var entry = zipStream.nextEntry
                    while (entry != null) {
                        if (entry.name.lowercase().endsWith(".kml")) {
                            kmlContent = zipStream.bufferedReader().readText()
                            break
                        }
                        entry = zipStream.nextEntry
                    }
                    zipStream.close()
                }
                if (kmlContent.isNotBlank()) {
                    parseKmlString(kmlContent)
                } else {
                    ParsedKmzResult(emptyList(), emptyList(), 0)
                }
            } else {
                val kmlText = inputStream.bufferedReader().readText()
                inputStream.close()
                parseKmlString(kmlText)
            }
        } catch (e: Exception) {
            Log.e("KmzParser", "Error parsing file: ${e.message}", e)
            ParsedKmzResult(emptyList(), emptyList(), 0)
        }
    }

    private fun isZipStream(inputStream: InputStream): Boolean {
        return try {
            inputStream.mark(4)
            val b = ByteArray(4)
            val read = inputStream.read(b, 0, 4)
            inputStream.reset()
            read == 4 && b[0] == 0x50.toByte() && b[1] == 0x4B.toByte()
        } catch (_: Exception) {
            false
        }
    }

    fun parseKmlString(kmlContent: String): ParsedKmzResult {
        val sitesMap = mutableMapOf<String, SiteEntity>()
        val sectorsList = mutableListOf<SectorEntity>()
        var placemarkCount = 0

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(kmlContent))

            var eventType = parser.eventType
            var inPlacemark = false
            var currentName = ""
            var currentDescription = ""
            var currentCoordsStr = ""
            val extendedDataMap = mutableMapOf<String, String>()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName.equals("Placemark", ignoreCase = true)) {
                            inPlacemark = true
                            currentName = ""
                            currentDescription = ""
                            currentCoordsStr = ""
                            extendedDataMap.clear()
                        } else if (inPlacemark) {
                            when {
                                tagName.equals("name", ignoreCase = true) -> {
                                    currentName = parser.nextText()
                                }
                                tagName.equals("description", ignoreCase = true) -> {
                                    currentDescription = parser.nextText()
                                }
                                tagName.equals("coordinates", ignoreCase = true) -> {
                                    currentCoordsStr = parser.nextText()
                                }
                                tagName.equals("Data", ignoreCase = true) || tagName.equals("SimpleData", ignoreCase = true) -> {
                                    val attrName = parser.getAttributeValue(null, "name") ?: ""
                                    val valText = parser.nextText()
                                    if (attrName.isNotBlank()) {
                                        extendedDataMap[attrName.lowercase()] = valText
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName.equals("Placemark", ignoreCase = true) && inPlacemark) {
                            inPlacemark = false
                            placemarkCount++

                            val coords = parseCoordinates(currentCoordsStr)
                            if (coords != null) {
                                val (lon, lat, alt) = coords

                                // Extract Site ID and Sector
                                val (siteId, sectorId, azimuth, band) = extractSiteSectorInfo(
                                    currentName,
                                    currentDescription,
                                    extendedDataMap
                                )

                                val siteName = "Site $siteId"
                                if (!sitesMap.containsKey(siteId)) {
                                    sitesMap[siteId] = SiteEntity(
                                        siteId = siteId,
                                        siteName = siteName,
                                        latitude = lat,
                                        longitude = lon,
                                        altitude = alt,
                                        technology = "LTE/5G"
                                    )
                                }

                                sectorsList.add(
                                    SectorEntity(
                                        siteId = siteId,
                                        sectorId = sectorId,
                                        sectorName = currentName.ifBlank { "Sector $sectorId" },
                                        azimuth = azimuth,
                                        beamwidth = 65f,
                                        band = band
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e("KmzParser", "XmlPullParser error: ${e.message}")
        }

        return ParsedKmzResult(
            sites = sitesMap.values.toList(),
            sectors = sectorsList,
            totalPlacemarks = placemarkCount
        )
    }

    private fun parseCoordinates(coordsStr: String): Triple<Double, Double, Double>? {
        try {
            val cleanStr = coordsStr.trim().replace("\n", " ")
            val parts = cleanStr.split(",").map { it.trim() }
            if (parts.size >= 2) {
                val lon = parts[0].toDoubleOrNull() ?: return null
                val lat = parts[1].toDoubleOrNull() ?: return null
                val alt = if (parts.size >= 3) parts[2].toDoubleOrNull() ?: 0.0 else 0.0
                return Triple(lon, lat, alt)
            }
        } catch (_: Exception) {}
        return null
    }

    private fun extractSiteSectorInfo(
        name: String,
        description: String,
        extMap: Map<String, String>
    ): Quad<String, String, Float, String> {
        var siteId = extMap["siteid"] ?: extMap["site_id"] ?: extMap["site"] ?: ""
        var sectorId = extMap["sectorid"] ?: extMap["sector_id"] ?: extMap["sector"] ?: ""
        var azimuthStr = extMap["azimuth"] ?: extMap["bearing"] ?: extMap["angle"] ?: ""
        var bandStr = extMap["band"] ?: extMap["freq"] ?: extMap["frequency"] ?: ""

        // Regex parsing on name if missing
        if (siteId.isBlank()) {
            val siteMatch = Regex("""(?:site|cell|tower)[_\s-]*([0-9a-zA-Z]+)""", RegexOption.IGNORE_CASE).find(name)
                ?: Regex("""^([0-9]{3,5})""", RegexOption.IGNORE_CASE).find(name)
            siteId = siteMatch?.groupValues?.get(1) ?: name.take(8).ifBlank { "101" }
        }

        if (sectorId.isBlank()) {
            val sectorMatch = Regex("""(?:sec|sector|alpha|beta|gamma)[_\s-]*([0-9a-zA-Z]+)""", RegexOption.IGNORE_CASE).find(name)
                ?: Regex("""[_-]([1-9])[a-zA-Z]?$""", RegexOption.IGNORE_CASE).find(name)
            sectorId = sectorMatch?.groupValues?.get(1) ?: "1"
        }

        if (azimuthStr.isBlank()) {
            val azMatch = Regex("""(?:azimuth|az|dir)[_\s:]*([0-9]{1,3})""", RegexOption.IGNORE_CASE).find(description)
            if (azMatch != null) azimuthStr = azMatch.groupValues[1]
        }

        if (bandStr.isBlank()) {
            val bandMatch = Regex("""(800|900|1800|2100|2600|B1|B3|B7|B8|B20|n78)""", RegexOption.IGNORE_CASE).find(name + " " + description)
            bandStr = bandMatch?.groupValues?.get(1) ?: "1800"
        }

        val azimuth = azimuthStr.toFloatOrNull() ?: when (sectorId.lowercase()) {
            "1", "a", "alpha" -> 0f
            "2", "b", "beta" -> 120f
            "3", "c", "gamma" -> 240f
            else -> 0f
        }

        return Quad(siteId, sectorId, azimuth, bandStr)
    }

    data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
