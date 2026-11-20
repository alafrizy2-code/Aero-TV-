package com.example.data.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data models for real-time European football match schedules, live scores,
 * team crests, and calendar dates from the ESPN Sports API.
 */
@JsonClass(generateAdapter = true)
data class EspnScoreboardResponse(
    @Json(name = "leagues") val leagues: List<EspnLeague>? = null,
    @Json(name = "events") val events: List<EspnEvent>? = null
)

@JsonClass(generateAdapter = true)
data class EspnLeague(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "abbreviation") val abbreviation: String? = null,
    @Json(name = "midsizeName") val midsizeName: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "logos") val logos: List<EspnLogo>? = null
)

@JsonClass(generateAdapter = true)
data class EspnLogo(
    @Json(name = "href") val href: String? = null,
    @Json(name = "width") val width: Int? = null,
    @Json(name = "height") val height: Int? = null,
    @Json(name = "alt") val alt: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnEvent(
    @Json(name = "id") val id: String? = null,
    @Json(name = "uid") val uid: String? = null,
    @Json(name = "date") val date: String? = null, // e.g. "2026-11-04T17:45Z"
    @Json(name = "name") val name: String? = null,
    @Json(name = "shortName") val shortName: String? = null,
    @Json(name = "status") val status: EspnStatus? = null,
    @Json(name = "competitions") val competitions: List<EspnCompetition>? = null
)

@JsonClass(generateAdapter = true)
data class EspnCompetition(
    @Json(name = "id") val id: String? = null,
    @Json(name = "date") val date: String? = null,
    @Json(name = "competitors") val competitors: List<EspnCompetitor>? = null,
    @Json(name = "status") val status: EspnStatus? = null
)

@JsonClass(generateAdapter = true)
data class EspnCompetitor(
    @Json(name = "id") val id: String? = null,
    @Json(name = "uid") val uid: String? = null,
    @Json(name = "homeAway") val homeAway: String? = null, // "home" or "away"
    @Json(name = "score") val score: String? = null,
    @Json(name = "team") val team: EspnTeam? = null,
    @Json(name = "winner") val winner: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class EspnTeam(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "shortDisplayName") val shortDisplayName: String? = null,
    @Json(name = "abbreviation") val abbreviation: String? = null,
    @Json(name = "logo") val logo: String? = null,
    @Json(name = "logos") val logos: List<EspnLogo>? = null
)

@JsonClass(generateAdapter = true)
data class EspnStatus(
    @Json(name = "displayClock") val displayClock: String? = null,
    @Json(name = "period") val period: Int? = null,
    @Json(name = "type") val type: EspnStatusType? = null
)

@JsonClass(generateAdapter = true)
data class EspnStatusType(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "state") val state: String? = null, // "pre", "in", "post"
    @Json(name = "completed") val completed: Boolean? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "detail") val detail: String? = null,
    @Json(name = "shortDetail") val shortDetail: String? = null
)
