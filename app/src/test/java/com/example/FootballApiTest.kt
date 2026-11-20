package com.example

import com.example.data.FootballScheduleRepository
import com.example.data.api.FootballApiService
import com.example.data.api.model.EspnCompetition
import com.example.data.api.model.EspnCompetitor
import com.example.data.api.model.EspnEvent
import com.example.data.api.model.EspnLeague
import com.example.data.api.model.EspnScoreboardResponse
import com.example.data.api.model.EspnStatus
import com.example.data.api.model.EspnStatusType
import com.example.data.api.model.EspnTeam
import com.example.model.MatchStatus
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FootballApiTest {

    @Test
    fun testMoshiJsonParsing() {
        val sampleJson = """
            {
              "leagues": [
                {
                  "id": "700",
                  "name": "English Premier League",
                  "abbreviation": "Premier League",
                  "calendar": ["2026-11-07T08:00Z"]
                }
              ],
              "events": [
                {
                  "id": "1001",
                  "date": "2026-11-07T12:30Z",
                  "name": "Arsenal at Chelsea",
                  "status": {
                    "displayClock": "65'",
                    "type": {
                      "state": "in",
                      "description": "In Progress"
                    }
                  },
                  "competitions": [
                    {
                      "id": "comp_1001",
                      "competitors": [
                        {
                          "homeAway": "home",
                          "score": "2",
                          "team": {
                            "name": "Chelsea",
                            "logo": "https://a.espncdn.com/i/teamlogos/soccer/500/363.png"
                          }
                        },
                        {
                          "homeAway": "away",
                          "score": "1",
                          "team": {
                            "name": "Arsenal",
                            "logo": "https://a.espncdn.com/i/teamlogos/soccer/500/359.png"
                          }
                        }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(EspnScoreboardResponse::class.java)
        val response = adapter.fromJson(sampleJson)

        assertNotNull(response)
        assertEquals(1, response?.leagues?.size)
        assertEquals("English Premier League", response?.leagues?.first()?.name)
        assertEquals(1, response?.events?.size)

        val event = response?.events?.first()
        assertEquals("1001", event?.id)
        assertEquals("65'", event?.status?.displayClock)
        assertEquals("in", event?.status?.type?.state)

        val comp = event?.competitions?.first()
        assertEquals(2, comp?.competitors?.size)
        assertEquals("Chelsea", comp?.competitors?.first()?.team?.name)
    }

    @Test
    fun testRepositoryWithMockApiService() = runBlocking {
        val mockService = object : FootballApiService {
            override suspend fun getScoreboard(league: String, dates: String?): EspnScoreboardResponse {
                return EspnScoreboardResponse(
                    leagues = listOf(
                        EspnLeague(
                            id = "700",
                            name = "Premier League"
                        )
                    ),
                    events = listOf(
                        EspnEvent(
                            id = "epl_test_1",
                            date = "2026-11-07T15:00Z",
                            name = "Arsenal at Chelsea",
                            status = EspnStatus(
                                displayClock = "72'",
                                type = EspnStatusType(state = "in")
                            ),
                            competitions = listOf(
                                EspnCompetition(
                                    id = "comp_test_1",
                                    competitors = listOf(
                                        EspnCompetitor(
                                            homeAway = "home",
                                            score = "2",
                                            team = EspnTeam(name = "Chelsea", logo = "https://example.com/chelsea.png")
                                        ),
                                        EspnCompetitor(
                                            homeAway = "away",
                                            score = "1",
                                            team = EspnTeam(name = "Arsenal", logo = "https://example.com/arsenal.png")
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            }
        }

        val repository = FootballScheduleRepository(apiService = mockService)
        val result = repository.getScheduleForLeague("eng.1")

        assertNotNull(result)
        assertEquals(1, result.matches.size)
        val match = result.matches.first()
        assertEquals("Chelsea", match.homeTeam)
        assertEquals("Arsenal", match.awayTeam)
        assertEquals(2, match.homeScore)
        assertEquals(1, match.awayScore)
        assertEquals(MatchStatus.LIVE, match.status)
        assertTrue(match.matchTimeWib.contains("WIB"))
    }
}
