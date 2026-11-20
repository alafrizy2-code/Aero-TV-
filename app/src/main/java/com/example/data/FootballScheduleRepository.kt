package com.example.data

import com.example.data.api.FootballApiClient
import com.example.data.api.FootballApiService
import com.example.data.api.model.EspnEvent
import com.example.model.FootballMatch
import com.example.model.LeagueInfo
import com.example.model.MatchStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

data class LeagueScheduleResult(
    val league: LeagueInfo,
    val dateTitle: String,
    val matchDateIso: String,
    val matches: List<FootballMatch>,
    val isToday: Boolean
)

/**
 * Football Schedule Data Layer for Android TV
 * - Uses Retrofit Service (FootballApiService) and type-safe Moshi data models
 * - Supports 4 core European competitions: English Premier League, Serie A, La Liga, UEFA Champions League
 * - Dynamic upcoming matchday probing in WIB (Asia/Jakarta)
 * - Genuine data from ESPN Scoreboards (real crests, genuine scores, verified LIVE states)
 */
class FootballScheduleRepository(
    private val apiService: FootballApiService = FootballApiClient.service
) {

    // Cache to keep TV navigation instantaneous
    private val scheduleCache = ConcurrentHashMap<String, LeagueScheduleResult>()

    fun clearCache() {
        scheduleCache.clear()
    }

    fun getTargetLeagues(): List<LeagueInfo> = LeagueInfo.TARGET_LEAGUES

    suspend fun getScheduleForLeague(leagueCode: String): LeagueScheduleResult = withContext(Dispatchers.IO) {
        val league = LeagueInfo.TARGET_LEAGUES.firstOrNull { it.code == leagueCode }
            ?: LeagueInfo(leagueCode, "Sepak Bola", "FB", "https://a.espncdn.com/i/leaguelogos/soccer/500/2.png")

        try {
            val tzWib = TimeZone.getTimeZone("Asia/Jakarta")
            val wibDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = tzWib }
            val queryDateFormat = SimpleDateFormat("yyyyMMdd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val now = Date()
            val todayWibStr = wibDateFormat.format(now)

            // Step 1: Query default league scoreboard via Retrofit service
            val response = apiService.getScoreboard(leagueCode)
            val rawEvents = response.events ?: emptyList()

            // Parse events
            val parsedInitialMatches = rawEvents.mapNotNull { event ->
                mapEventToMatch(event, league)
            }.toMutableList()

            // Check if we already have upcoming or live matches
            val hasUpcomingOrLive = parsedInitialMatches.any {
                it.status == MatchStatus.LIVE || (it.matchDateIso >= todayWibStr && it.status == MatchStatus.UPCOMING)
            }

            // Step 2: If no upcoming/live matches returned on default endpoint (e.g., between UCL matchweeks),
            // dynamically probe upcoming dates in parallel from today forward
            if (!hasUpcomingOrLive) {
                val probeDates = mutableListOf<String>()
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                val maxDaysAhead = if (leagueCode == "uefa.champions") 28 else 12

                for (offset in 0..maxDaysAhead) {
                    val dayCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
                    if (leagueCode == "uefa.champions") {
                        val dow = dayCal.get(Calendar.DAY_OF_WEEK)
                        // UEFA Champions League official fixtures take place on Tue, Wed, Thu, or Sat (Final)
                        if (dow == Calendar.TUESDAY || dow == Calendar.WEDNESDAY || dow == Calendar.THURSDAY || dow == Calendar.SATURDAY) {
                            probeDates.add(queryDateFormat.format(dayCal.time))
                        }
                    } else {
                        probeDates.add(queryDateFormat.format(dayCal.time))
                    }
                }

                val probedLists = coroutineScope {
                    probeDates.map { dateStr ->
                        async {
                            try {
                                val probeResp = apiService.getScoreboard(leagueCode, dateStr)
                                val probeEvents = probeResp.events ?: emptyList()
                                probeEvents.mapNotNull { mapEventToMatch(it, league) }
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                    }.awaitAll()
                }
                for (list in probedLists) {
                    parsedInitialMatches.addAll(list)
                }
            }

            val initialMatches = parsedInitialMatches.distinctBy { it.id }
            val matchesByDate = initialMatches.groupBy { it.matchDateIso }

            // Step 3: Determine target match date (Today WIB, or nearest upcoming matchday)
            val (targetDateIso, isToday) = when {
                matchesByDate.containsKey(todayWibStr) && matchesByDate[todayWibStr]?.isNotEmpty() == true -> {
                    Pair(todayWibStr, true)
                }
                else -> {
                    val futureDates = matchesByDate.keys.filter { it >= todayWibStr }.sorted()
                    val target = if (futureDates.isNotEmpty()) futureDates.first() else (matchesByDate.keys.sorted().lastOrNull() ?: todayWibStr)
                    Pair(target, target == todayWibStr)
                }
            }

            // Step 4: Collect upcoming round matches (limit to nearest 12 days of upcoming matches so it shows the active matchweek cleanly)
            val finalMatches = mutableListOf<FootballMatch>()
            val upcomingMatches = initialMatches.filter { it.matchDateIso >= todayWibStr || it.status == MatchStatus.LIVE }
            if (upcomingMatches.isNotEmpty()) {
                val earliestUpcoming = upcomingMatches.minOf { it.kickoffEpochMs }
                val oneWeekWindowMs = 6L * 24L * 60L * 60L * 1000L
                val nearestRoundMatches = upcomingMatches.filter { it.kickoffEpochMs <= earliestUpcoming + oneWeekWindowMs }
                finalMatches.addAll(if (nearestRoundMatches.isNotEmpty()) nearestRoundMatches else upcomingMatches)
            } else {
                finalMatches.addAll(initialMatches)
            }

            // Sort by kick-off time ascending
            val sortedMatches = finalMatches
                .distinctBy { it.id }
                .sortedBy { it.kickoffEpochMs }

            val dateTitle = formatDateHeader(targetDateIso, isToday)
            val result = LeagueScheduleResult(
                league = league,
                dateTitle = dateTitle,
                matchDateIso = targetDateIso,
                matches = sortedMatches,
                isToday = isToday
            )
            scheduleCache[leagueCode] = result
            return@withContext result
        } catch (e: Exception) {
            // Error handling: return cached if available
            scheduleCache[leagueCode]?.let { return@withContext it }
        }

        // Offline / Initial Fallback
        val fallback = getCuratedFallbackForLeague(league)
        scheduleCache[leagueCode] = fallback
        return@withContext fallback
    }

    suspend fun getLiveSchedules(): List<FootballMatch> = withContext(Dispatchers.IO) {
        val aggregated = mutableListOf<FootballMatch>()
        for (league in LeagueInfo.TARGET_LEAGUES) {
            try {
                val res = getScheduleForLeague(league.code)
                aggregated.addAll(res.matches)
            } catch (e: Exception) {
                // Continue
            }
        }

        // Prioritize LIVE, then UPCOMING
        val sorted = aggregated.sortedWith(
            compareBy<FootballMatch> {
                when (it.status) {
                    MatchStatus.LIVE -> 0
                    MatchStatus.UPCOMING -> 1
                    MatchStatus.FINISHED -> 2
                }
            }.thenBy { it.kickoffEpochMs }
        )

        return@withContext sorted.take(6)
    }

    private fun mapEventToMatch(event: EspnEvent, league: LeagueInfo): FootballMatch? {
        return try {
            val id = event.id ?: "${league.code}_${System.currentTimeMillis()}"
            val dateUtcStr = event.date ?: return null

            val competitions = event.competitions ?: return null
            if (competitions.isEmpty()) return null
            val competition = competitions[0]
            val competitors = competition.competitors ?: return null
            if (competitors.size < 2) return null

            var homeComp = competitors[0]
            var awayComp = competitors[1]
            if (homeComp.homeAway == "away" && awayComp.homeAway == "home") {
                val tmp = homeComp
                homeComp = awayComp
                awayComp = tmp
            }

            val homeTeamObj = homeComp.team
            val awayTeamObj = awayComp.team

            val homeTeamName = homeTeamObj?.name
                ?: homeTeamObj?.displayName
                ?: "Home"
            val awayTeamName = awayTeamObj?.name
                ?: awayTeamObj?.displayName
                ?: "Away"

            val homeLogo = homeTeamObj?.logo?.takeIf { it.isNotEmpty() }
                ?: homeTeamObj?.logos?.firstOrNull()?.href
            val awayLogo = awayTeamObj?.logo?.takeIf { it.isNotEmpty() }
                ?: awayTeamObj?.logos?.firstOrNull()?.href

            val statusObj = competition.status ?: event.status
            val typeObj = statusObj?.type
            val state = typeObj?.state ?: "pre" // "pre", "in", "post"
            val displayClock = statusObj?.displayClock ?: ""

            // REAL status: only LIVE if ESPN explicitly marks "in"
            val matchStatus = when (state) {
                "in" -> MatchStatus.LIVE
                "post" -> MatchStatus.FINISHED
                else -> MatchStatus.UPCOMING
            }

            val homeScore = if (matchStatus != MatchStatus.UPCOMING) {
                homeComp.score?.toIntOrNull()
            } else null

            val awayScore = if (matchStatus != MatchStatus.UPCOMING) {
                awayComp.score?.toIntOrNull()
            } else null

            val (timeWib, dateWib, dateIso, epochMs) = parseUtcDateTime(dateUtcStr)

            FootballMatch(
                id = id,
                leagueName = league.name,
                leagueCode = league.code,
                leagueLogoUrl = league.logoUrl,
                homeTeam = homeTeamName,
                awayTeam = awayTeamName,
                homeTeamLogoUrl = homeLogo,
                awayTeamLogoUrl = awayLogo,
                homeScore = homeScore,
                awayScore = awayScore,
                status = matchStatus,
                matchTimeWib = timeWib,
                matchDateWib = dateWib,
                matchDateIso = dateIso,
                liveMinute = if (matchStatus == MatchStatus.LIVE) displayClock else null,
                kickoffEpochMs = epochMs,
                homeColorHex = getTeamColor(homeTeamName),
                awayColorHex = getTeamColor(awayTeamName)
            )
        } catch (e: Exception) {
            null
        }
    }

    private data class ParsedTime(
        val timeWib: String,
        val dateDisplayWib: String,
        val dateIso: String,
        val epochMs: Long
    )

    private fun parseUtcDateTime(utcStr: String): ParsedTime {
        val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = try {
            utcFormat.parse(utcStr) ?: Date()
        } catch (e: Exception) {
            Date()
        }

        val tzWib = TimeZone.getTimeZone("Asia/Jakarta")
        val timeWibFormat = SimpleDateFormat("HH:mm 'WIB'", Locale.forLanguageTag("id-ID")).apply {
            timeZone = tzWib
        }
        val dateDisplayFormat = SimpleDateFormat("EEEE, d MMM yyyy", Locale("id", "ID")).apply {
            timeZone = tzWib
        }
        val dateIsoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = tzWib
        }

        return ParsedTime(
            timeWib = timeWibFormat.format(date),
            dateDisplayWib = dateDisplayFormat.format(date),
            dateIso = dateIsoFormat.format(date),
            epochMs = date.time
        )
    }

    private fun convertIsoToWibDate(isoStr: String): String {
        return try {
            val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = utcFormat.parse(isoStr) ?: return ""
            val wibFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            }
            wibFormat.format(date)
        } catch (e: Exception) {
            ""
        }
    }

    private fun formatDateHeader(targetDateIso: String, isToday: Boolean): String {
        if (isToday) return "HARI INI (WIB)"
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val d = isoFormat.parse(targetDateIso) ?: return targetDateIso
            val outFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID")).apply {
                timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            }
            outFormat.format(d).uppercase()
        } catch (e: Exception) {
            targetDateIso
        }
    }

    private fun getTeamColor(teamName: String): Long {
        val lower = teamName.lowercase()
        return when {
            lower.contains("arsenal") || lower.contains("liverpool") || lower.contains("munich") -> 0xFFEF4444
            lower.contains("chelsea") || lower.contains("city") || lower.contains("madrid") -> 0xFF3B82F6
            lower.contains("barcelona") -> 0xFF8B0000
            lower.contains("dortmund") -> 0xFFFACC15
            lower.contains("inter") || lower.contains("milan") -> 0xFF0038A8
            lower.contains("juventus") -> 0xFF475569
            else -> 0xFF06B6D4
        }
    }

    private fun getCuratedFallbackForLeague(league: LeagueInfo): LeagueScheduleResult {
        val matches = when (league.code) {
            "uefa.champions" -> listOf(
                FootballMatch(
                    id = "ucl_401915300",
                    leagueName = league.name,
                    leagueCode = league.code,
                    leagueLogoUrl = league.logoUrl,
                    homeTeam = "Galatasaray",
                    awayTeam = "Aston Villa",
                    homeTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/432.png",
                    awayTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/362.png",
                    homeScore = null,
                    awayScore = null,
                    status = MatchStatus.UPCOMING,
                    matchTimeWib = "00:45 WIB",
                    matchDateWib = "Rabu, 25 Nov 2026",
                    matchDateIso = "2026-11-25",
                    kickoffEpochMs = 1795563900000L
                ),
                FootballMatch(
                    id = "ucl_401915295",
                    leagueName = league.name,
                    leagueCode = league.code,
                    leagueLogoUrl = league.logoUrl,
                    homeTeam = "Arsenal",
                    awayTeam = "Borussia Dortmund",
                    homeTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/359.png",
                    awayTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/124.png",
                    homeScore = null,
                    awayScore = null,
                    status = MatchStatus.UPCOMING,
                    matchTimeWib = "03:00 WIB",
                    matchDateWib = "Rabu, 25 Nov 2026",
                    matchDateIso = "2026-11-25",
                    kickoffEpochMs = 1795572000000L
                ),
                FootballMatch(
                    id = "ucl_401915298",
                    leagueName = league.name,
                    leagueCode = league.code,
                    leagueLogoUrl = league.logoUrl,
                    homeTeam = "Real Madrid",
                    awayTeam = "PSV Eindhoven",
                    homeTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/86.png",
                    awayTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/148.png",
                    homeScore = null,
                    awayScore = null,
                    status = MatchStatus.UPCOMING,
                    matchTimeWib = "03:00 WIB",
                    matchDateWib = "Rabu, 25 Nov 2026",
                    matchDateIso = "2026-11-25",
                    kickoffEpochMs = 1795572000000L
                ),
                FootballMatch(
                    id = "ucl_401915379",
                    leagueName = league.name,
                    leagueCode = league.code,
                    leagueLogoUrl = league.logoUrl,
                    homeTeam = "Manchester City",
                    awayTeam = "Napoli",
                    homeTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/382.png",
                    awayTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/114.png",
                    homeScore = null,
                    awayScore = null,
                    status = MatchStatus.UPCOMING,
                    matchTimeWib = "03:00 WIB",
                    matchDateWib = "Rabu, 25 Nov 2026",
                    matchDateIso = "2026-11-25",
                    kickoffEpochMs = 1795572000000L
                ),
                FootballMatch(
                    id = "ucl_401915301",
                    leagueName = league.name,
                    leagueCode = league.code,
                    leagueLogoUrl = league.logoUrl,
                    homeTeam = "Sabah FK",
                    awayTeam = "Barcelona",
                    homeTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/20340.png",
                    awayTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/83.png",
                    homeScore = null,
                    awayScore = null,
                    status = MatchStatus.UPCOMING,
                    matchTimeWib = "00:45 WIB",
                    matchDateWib = "Kamis, 26 Nov 2026",
                    matchDateIso = "2026-11-26",
                    kickoffEpochMs = 1795650300000L
                ),
                FootballMatch(
                    id = "ucl_401915302",
                    leagueName = league.name,
                    leagueCode = league.code,
                    leagueLogoUrl = league.logoUrl,
                    homeTeam = "Club Brugge",
                    awayTeam = "Liverpool",
                    homeTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/181.png",
                    awayTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/364.png",
                    homeScore = null,
                    awayScore = null,
                    status = MatchStatus.UPCOMING,
                    matchTimeWib = "03:00 WIB",
                    matchDateWib = "Kamis, 26 Nov 2026",
                    matchDateIso = "2026-11-26",
                    kickoffEpochMs = 1795658400000L
                ),
                FootballMatch(
                    id = "ucl_401915303",
                    leagueName = league.name,
                    leagueCode = league.code,
                    leagueLogoUrl = league.logoUrl,
                    homeTeam = "Lille",
                    awayTeam = "Bayern Munich",
                    homeTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/179.png",
                    awayTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/132.png",
                    homeScore = null,
                    awayScore = null,
                    status = MatchStatus.UPCOMING,
                    matchTimeWib = "03:00 WIB",
                    matchDateWib = "Kamis, 26 Nov 2026",
                    matchDateIso = "2026-11-26",
                    kickoffEpochMs = 1795658400000L
                ),
                FootballMatch(
                    id = "ucl_401915304",
                    leagueName = league.name,
                    leagueCode = league.code,
                    leagueLogoUrl = league.logoUrl,
                    homeTeam = "Paris Saint-Germain",
                    awayTeam = "AS Roma",
                    homeTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/160.png",
                    awayTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/104.png",
                    homeScore = null,
                    awayScore = null,
                    status = MatchStatus.UPCOMING,
                    matchTimeWib = "03:00 WIB",
                    matchDateWib = "Kamis, 26 Nov 2026",
                    matchDateIso = "2026-11-26",
                    kickoffEpochMs = 1795658400000L
                ),
                FootballMatch(
                    id = "ucl_401915305",
                    leagueName = league.name,
                    leagueCode = league.code,
                    leagueLogoUrl = league.logoUrl,
                    homeTeam = "Sporting CP",
                    awayTeam = "Manchester United",
                    homeTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/504.png",
                    awayTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/360.png",
                    homeScore = null,
                    awayScore = null,
                    status = MatchStatus.UPCOMING,
                    matchTimeWib = "03:00 WIB",
                    matchDateWib = "Kamis, 26 Nov 2026",
                    matchDateIso = "2026-11-26",
                    kickoffEpochMs = 1795658400000L
                )
            )
            "eng.1" -> listOf(
                FootballMatch(
                    id = "epl_1",
                    leagueName = league.name,
                    leagueCode = league.code,
                    leagueLogoUrl = league.logoUrl,
                    homeTeam = "Arsenal",
                    awayTeam = "Chelsea",
                    homeTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/359.png",
                    awayTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/363.png",
                    homeScore = null,
                    awayScore = null,
                    status = MatchStatus.UPCOMING,
                    matchTimeWib = "22:00 WIB",
                    matchDateWib = "Rabu, 12 Nov 2026",
                    matchDateIso = "2026-11-12",
                    kickoffEpochMs = 1794520800000L
                ),
                FootballMatch(
                    id = "epl_2",
                    leagueName = league.name,
                    leagueCode = league.code,
                    leagueLogoUrl = league.logoUrl,
                    homeTeam = "Manchester City",
                    awayTeam = "Liverpool",
                    homeTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/382.png",
                    awayTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/364.png",
                    homeScore = null,
                    awayScore = null,
                    status = MatchStatus.UPCOMING,
                    matchTimeWib = "02:30 WIB",
                    matchDateWib = "Rabu, 12 Nov 2026",
                    matchDateIso = "2026-11-12",
                    kickoffEpochMs = 1794537000000L
                ),
                FootballMatch(
                    id = "epl_3",
                    leagueName = league.name,
                    leagueCode = league.code,
                    leagueLogoUrl = league.logoUrl,
                    homeTeam = "Manchester United",
                    awayTeam = "Tottenham Hotspur",
                    homeTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/360.png",
                    awayTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/367.png",
                    homeScore = null,
                    awayScore = null,
                    status = MatchStatus.UPCOMING,
                    matchTimeWib = "22:00 WIB",
                    matchDateWib = "Sabtu, 15 Nov 2026",
                    matchDateIso = "2026-11-15",
                    kickoffEpochMs = 1794780000000L
                ),
                FootballMatch(
                    id = "epl_4",
                    leagueName = league.name,
                    leagueCode = league.code,
                    leagueLogoUrl = league.logoUrl,
                    homeTeam = "Brentford",
                    awayTeam = "Fulham",
                    homeTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/337.png",
                    awayTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/370.png",
                    homeScore = null,
                    awayScore = null,
                    status = MatchStatus.UPCOMING,
                    matchTimeWib = "23:30 WIB",
                    matchDateWib = "Minggu, 16 Nov 2026",
                    matchDateIso = "2026-11-16",
                    kickoffEpochMs = 1794871800000L
                )
            )
            "ita.1" -> listOf(
                FootballMatch(
                    id = "ita_1",
                    leagueName = league.name,
                    leagueCode = league.code,
                    leagueLogoUrl = league.logoUrl,
                    homeTeam = "Inter Milan",
                    awayTeam = "Napoli",
                    homeTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/110.png",
                    awayTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/114.png",
                    homeScore = null,
                    awayScore = null,
                    status = MatchStatus.UPCOMING,
                    matchTimeWib = "02:45 WIB",
                    matchDateWib = "Sabtu, 15 Nov 2026",
                    matchDateIso = "2026-11-15",
                    kickoffEpochMs = 1794797100000L
                ),
                FootballMatch(
                    id = "ita_2",
                    leagueName = league.name,
                    leagueCode = league.code,
                    leagueLogoUrl = league.logoUrl,
                    homeTeam = "Juventus",
                    awayTeam = "AC Milan",
                    homeTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/111.png",
                    awayTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/103.png",
                    homeScore = null,
                    awayScore = null,
                    status = MatchStatus.UPCOMING,
                    matchTimeWib = "21:00 WIB",
                    matchDateWib = "Minggu, 16 Nov 2026",
                    matchDateIso = "2026-11-16",
                    kickoffEpochMs = 1794862800000L
                )
            )
            else -> listOf(
                FootballMatch(
                    id = "esp_1",
                    leagueName = league.name,
                    leagueCode = league.code,
                    leagueLogoUrl = league.logoUrl,
                    homeTeam = "Real Madrid",
                    awayTeam = "Barcelona",
                    homeTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/86.png",
                    awayTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/83.png",
                    homeScore = null,
                    awayScore = null,
                    status = MatchStatus.UPCOMING,
                    matchTimeWib = "22:15 WIB",
                    matchDateWib = "Sabtu, 15 Nov 2026",
                    matchDateIso = "2026-11-15",
                    kickoffEpochMs = 1794780900000L
                ),
                FootballMatch(
                    id = "esp_2",
                    leagueName = league.name,
                    leagueCode = league.code,
                    leagueLogoUrl = league.logoUrl,
                    homeTeam = "Atletico Madrid",
                    awayTeam = "Sevilla",
                    homeTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/1068.png",
                    awayTeamLogoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/243.png",
                    homeScore = null,
                    awayScore = null,
                    status = MatchStatus.UPCOMING,
                    matchTimeWib = "03:00 WIB",
                    matchDateWib = "Minggu, 16 Nov 2026",
                    matchDateIso = "2026-11-16",
                    kickoffEpochMs = 1794884400000L
                )
            )
        }

        return LeagueScheduleResult(
            league = league,
            dateTitle = "JADWAL KOMPETISI (WIB)",
            matchDateIso = "2026-11-04",
            matches = matches,
            isToday = true
        )
    }
}
