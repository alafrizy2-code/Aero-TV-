package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.FootballMatch
import com.example.model.MatchStatus
import com.example.model.WeatherInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Aero TV", appName)
  }

  @Test
  fun `verify football match model with WIB time`() {
    val match = FootballMatch(
      id = "test_1",
      leagueName = "Premier League",
      leagueCode = "eng.1",
      homeTeam = "Arsenal",
      awayTeam = "Chelsea",
      homeScore = 2,
      awayScore = 1,
      status = MatchStatus.LIVE,
      matchTimeWib = "20:30 WIB",
      matchDateWib = "Hari Ini",
      liveMinute = "75'"
    )

    assertEquals("Arsenal", match.homeTeam)
    assertEquals("Chelsea", match.awayTeam)
    assertEquals("20:30 WIB", match.matchTimeWib)
    assertEquals(MatchStatus.LIVE, match.status)
  }

  @Test
  fun `verify weather info data model`() {
    val weather = WeatherInfo(
      temperature = 31,
      conditionText = "Cerah Berawan",
      conditionCode = 2,
      locationName = "Jakarta (WIB)",
      humidity = 68,
      windSpeed = 14,
      isDay = true,
      lastUpdated = "19:30 WIB"
    )

    assertEquals(31, weather.temperature)
    assertEquals("Jakarta (WIB)", weather.locationName)
    assertNotNull(weather.lastUpdated)
  }

  @Test
  fun `verify DateTimeHelper formats date as DD-MM-YYYY`() {
    val formattedDate = DateTimeHelper.getFormattedDate()
    assertNotNull(formattedDate)
    assertTrue(formattedDate.matches(Regex("\\d{2}-\\d{2}-\\d{4}")))
  }
}
