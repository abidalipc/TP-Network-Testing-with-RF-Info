package com.example.data.kmz

import com.example.data.db.SectorEntity
import com.example.data.db.SiteEntity

object SampleKmzData {

    fun getSampleKmlXml(): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2">
  <Document>
    <name>Metropolitan Telecom Network Sites &amp; Sectors</name>
    <description>Cellular Tower Infrastructure across 800, 900, 1800, 2100 &amp; 2600 MHz bands</description>
    
    <!-- Site 104: Central Tower -->
    <Placemark>
      <name>Site_104_Sec_1_B20</name>
      <description>Central Plaza Tower - Sector 1 Band 800MHz</description>
      <ExtendedData>
        <Data name="SiteID"><value>104</value></Data>
        <Data name="SectorID"><value>1</value></Data>
        <Data name="Azimuth"><value>0</value></Data>
        <Data name="Band"><value>800</value></Data>
      </ExtendedData>
      <Point><coordinates>101.6869,3.1390,45.0</coordinates></Point>
    </Placemark>

    <Placemark>
      <name>Site_104_Sec_2_B3</name>
      <description>Central Plaza Tower - Sector 2 Band 1800MHz</description>
      <ExtendedData>
        <Data name="SiteID"><value>104</value></Data>
        <Data name="SectorID"><value>2</value></Data>
        <Data name="Azimuth"><value>120</value></Data>
        <Data name="Band"><value>1800</value></Data>
      </ExtendedData>
      <Point><coordinates>101.6869,3.1390,45.0</coordinates></Point>
    </Placemark>

    <Placemark>
      <name>Site_104_Sec_3_B7</name>
      <description>Central Plaza Tower - Sector 3 Band 2600MHz</description>
      <ExtendedData>
        <Data name="SiteID"><value>104</value></Data>
        <Data name="SectorID"><value>3</value></Data>
        <Data name="Azimuth"><value>240</value></Data>
        <Data name="Band"><value>2600</value></Data>
      </ExtendedData>
      <Point><coordinates>101.6869,3.1390,45.0</coordinates></Point>
    </Placemark>

    <!-- Site 105: Metro Heights -->
    <Placemark>
      <name>Site_105_Sec_1_B8</name>
      <description>Metro Heights Antenna - Sector 1 Band 900MHz</description>
      <ExtendedData>
        <Data name="SiteID"><value>105</value></Data>
        <Data name="SectorID"><value>1</value></Data>
        <Data name="Azimuth"><value>30</value></Data>
        <Data name="Band"><value>900</value></Data>
      </ExtendedData>
      <Point><coordinates>101.6925,3.1435,52.0</coordinates></Point>
    </Placemark>

    <Placemark>
      <name>Site_105_Sec_2_B1</name>
      <description>Metro Heights Antenna - Sector 2 Band 2100MHz</description>
      <ExtendedData>
        <Data name="SiteID"><value>105</value></Data>
        <Data name="SectorID"><value>2</value></Data>
        <Data name="Azimuth"><value>150</value></Data>
        <Data name="Band"><value>2100</value></Data>
      </ExtendedData>
      <Point><coordinates>101.6925,3.1435,52.0</coordinates></Point>
    </Placemark>

    <Placemark>
      <name>Site_105_Sec_3_B3</name>
      <description>Metro Heights Antenna - Sector 3 Band 1800MHz</description>
      <ExtendedData>
        <Data name="SiteID"><value>105</value></Data>
        <Data name="SectorID"><value>3</value></Data>
        <Data name="Azimuth"><value>270</value></Data>
        <Data name="Band"><value>1800</value></Data>
      </ExtendedData>
      <Point><coordinates>101.6925,3.1435,52.0</coordinates></Point>
    </Placemark>

    <!-- Site 106: Riverside Microcell -->
    <Placemark>
      <name>Site_106_Sec_1_B7</name>
      <description>Riverside Station - Sector 1 Band 2600MHz High Capacity</description>
      <ExtendedData>
        <Data name="SiteID"><value>106</value></Data>
        <Data name="SectorID"><value>1</value></Data>
        <Data name="Azimuth"><value>45</value></Data>
        <Data name="Band"><value>2600</value></Data>
      </ExtendedData>
      <Point><coordinates>101.6810,3.1320,38.0</coordinates></Point>
    </Placemark>

    <Placemark>
      <name>Site_106_Sec_2_B20</name>
      <description>Riverside Station - Sector 2 Band 800MHz Coverage</description>
      <ExtendedData>
        <Data name="SiteID"><value>106</value></Data>
        <Data name="SectorID"><value>2</value></Data>
        <Data name="Azimuth"><value>225</value></Data>
        <Data name="Band"><value>800</value></Data>
      </ExtendedData>
      <Point><coordinates>101.6810,3.1320,38.0</coordinates></Point>
    </Placemark>

  </Document>
</kml>""".trimIndent()
    }

    fun getSampleSitesAndSectors(): Pair<List<SiteEntity>, List<SectorEntity>> {
        val sites = listOf(
            SiteEntity("Khwazakhela", "Khwazakhela", 34.9350, 72.4580, 45.0, "LTE / 5G", "Matta & Khwaza Khela Rd"),
            SiteEntity("Chalyar", "Chalyar", 34.9620, 72.4510, 52.0, "LTE / 5G", "Chalyar North"),
            SiteEntity("Tekdarai", "Tekdarai", 34.9450, 72.4400, 38.0, "LTE / 5G", "Tekdarai West"),
            SiteEntity("Langer", "Langer", 34.9400, 72.4720, 40.0, "LTE / 5G", "Langer East"),
            SiteEntity("Bandai_Wazakhela", "Bandai wazakhela", 34.9120, 72.4450, 50.0, "LTE / 5G", "Bandai Road"),
            SiteEntity("104", "Central Plaza Tower", 3.1390, 101.6869, 45.0, "LTE / 5G", "104 Central Ave"),
            SiteEntity("105", "Metro Heights Tower", 3.1435, 101.6925, 52.0, "LTE / 5G", "88 Heights Blvd")
        )

        val sectors = listOf(
            // Khwazakhela
            SectorEntity(siteId = "Khwazakhela", sectorId = "1", sectorName = "Khwazakhela Alpha (1800MHz)", azimuth = 0f, beamwidth = 65f, band = "1800", earfcn = 1300, pci = 101),
            SectorEntity(siteId = "Khwazakhela", sectorId = "2", sectorName = "Khwazakhela Beta (2100MHz)", azimuth = 120f, beamwidth = 65f, band = "2100", earfcn = 300, pci = 102),
            SectorEntity(siteId = "Khwazakhela", sectorId = "3", sectorName = "Khwazakhela Gamma (900MHz)", azimuth = 240f, beamwidth = 65f, band = "900", earfcn = 3500, pci = 103),

            // Chalyar
            SectorEntity(siteId = "Chalyar", sectorId = "1", sectorName = "Chalyar Alpha (1800MHz)", azimuth = 30f, beamwidth = 65f, band = "1800", earfcn = 1300, pci = 201),
            SectorEntity(siteId = "Chalyar", sectorId = "2", sectorName = "Chalyar Beta (800MHz)", azimuth = 180f, beamwidth = 65f, band = "800", earfcn = 6200, pci = 202),

            // Tekdarai
            SectorEntity(siteId = "Tekdarai", sectorId = "1", sectorName = "Tekdarai Alpha (2600MHz)", azimuth = 45f, beamwidth = 65f, band = "2600", earfcn = 2850, pci = 301),

            // Langer
            SectorEntity(siteId = "Langer", sectorId = "1", sectorName = "Langer Alpha (2100MHz)", azimuth = 90f, beamwidth = 65f, band = "2100", earfcn = 300, pci = 401),

            // Bandai Wazakhela
            SectorEntity(siteId = "Bandai_Wazakhela", sectorId = "1", sectorName = "Bandai Alpha (1800MHz)", azimuth = 150f, beamwidth = 65f, band = "1800", earfcn = 1300, pci = 501),

            // Site 104
            SectorEntity(siteId = "104", sectorId = "1", sectorName = "104 Alpha (800MHz)", azimuth = 0f, beamwidth = 65f, band = "800", earfcn = 6200, pci = 1041)
        )

        return Pair(sites, sectors)
    }
}
