package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.SubtitleSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Whisper SRT", appName)
  }

  @Test
  fun `test srt formatting`() {
    val segment1 = SubtitleSegment(1, 1000L, 3500L, "Halo dunia")
    val segment2 = SubtitleSegment(2, 4000L, 7250L, "Transkripsi Whisper real-time")

    val srt = SubtitleSegment.toFullSrt(listOf(segment1, segment2))
    assertTrue(srt.contains("00:00:01,000 --> 00:00:03,500"))
    assertTrue(srt.contains("Halo dunia"))
    assertTrue(srt.contains("00:00:04,000 --> 00:00:07,250"))
    assertTrue(srt.contains("Transkripsi Whisper real-time"))

    val parsed = SubtitleSegment.parseSrt(srt)
    assertEquals(2, parsed.size)
    assertEquals("Halo dunia", parsed[0].text)
    assertEquals(1000L, parsed[0].startTimeMs)
    assertEquals(3500L, parsed[0].endTimeMs)
  }
}
