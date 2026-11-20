package com.example.data.api

import com.example.data.api.model.EspnScoreboardResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service definition to fetch real-time European football match schedules,
 * live scores, status, and upcoming fixtures from an external sports API.
 */
interface FootballApiService {

    /**
     * Fetch scoreboard for a specific soccer league.
     * Supported European leagues:
     * - "uefa.champions": UEFA Champions League
     * - "eng.1": English Premier League
     * - "ita.1": Italian Serie A
     * - "esp.1": Spanish La Liga
     *
     * @param league League identifier slug
     * @param dates Optional date filter in YYYYMMDD format (e.g., "20261107")
     */
    @GET("apis/site/v2/sports/soccer/{league}/scoreboard")
    suspend fun getScoreboard(
        @Path("league") league: String,
        @Query("dates") dates: String? = null
    ): EspnScoreboardResponse
}
