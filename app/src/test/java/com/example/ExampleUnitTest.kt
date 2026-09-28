package com.example

import com.example.telecom.BandHelper
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testBandDetection() {
    val b800 = BandHelper.getLteBandFromEarfcn(6200)
    assertEquals("Band 20", b800.bandNumber)
    assertEquals("800 MHz", b800.frequencyLabel)

    val b900 = BandHelper.getLteBandFromEarfcn(3500)
    assertEquals("Band 8", b900.bandNumber)
    assertEquals("900 MHz", b900.frequencyLabel)

    val b1800 = BandHelper.getLteBandFromEarfcn(1300)
    assertEquals("Band 3", b1800.bandNumber)
    assertEquals("1800 MHz", b1800.frequencyLabel)

    val b2100 = BandHelper.getLteBandFromEarfcn(300)
    assertEquals("Band 1", b2100.bandNumber)
    assertEquals("2100 MHz", b2100.frequencyLabel)

    val b2600 = BandHelper.getLteBandFromEarfcn(2850)
    assertEquals("Band 7", b2600.bandNumber)
    assertEquals("2600 MHz", b2600.frequencyLabel)
  }
}
