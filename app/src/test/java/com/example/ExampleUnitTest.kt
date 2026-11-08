package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testWhisperModelPresetsCount() {
    val presets = com.example.model.WhisperModelPreset.ALL_PRESETS
    assertTrue("Harus memiliki 30 model preset", presets.size >= 30)
    assertNotNull(presets.find { it.id == "tiny" })
    assertNotNull(presets.find { it.id == "large-v3-turbo" })
  }

  @Test
  fun testSubtitleFormatting() {
    val seg = com.example.model.SubtitleSegment(1, 0, 1500, "Halo dunia")
    val srt = seg.toSrtBlock()
    assertTrue(srt.contains("00:00:00,000 --> 00:00:01,500"))
    assertTrue(srt.contains("Halo dunia"))
  }
}
