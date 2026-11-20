package com.example.model

enum class MatchStatus {
    LIVE,
    UPCOMING,
    FINISHED
}

data class LeagueInfo(
    val code: String,
    val name: String,
    val shortName: String,
    val logoUrl: String,
    val country: String = "Eropa"
) {
    companion object {
        // Urutan 4 Liga Sesuai Permintaan:
        // 1. English Premier League
        // 2. Serie A
        // 3. La Liga
        // 4. UEFA Champions League
        val TARGET_LEAGUES = listOf(
            LeagueInfo(
                code = "eng.1",
                name = "English Premier League",
                shortName = "EPL",
                logoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/23.png",
                country = "Inggris"
            ),
            LeagueInfo(
                code = "ita.1",
                name = "Serie A",
                shortName = "Serie A",
                logoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/12.png",
                country = "Italia"
            ),
            LeagueInfo(
                code = "esp.1",
                name = "La Liga",
                shortName = "La Liga",
                logoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/15.png",
                country = "Spanyol"
            ),
            LeagueInfo(
                code = "uefa.champions",
                name = "UEFA Champions League",
                shortName = "UCL",
                logoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/2.png",
                country = "Eropa"
            )
        )
    }
}

data class FootballMatch(
    val id: String,
    val leagueName: String,
    val leagueCode: String,
    val leagueLogoUrl: String? = null,
    val homeTeam: String,
    val awayTeam: String,
    val homeTeamLogoUrl: String? = null,
    val awayTeamLogoUrl: String? = null,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val status: MatchStatus = MatchStatus.UPCOMING,
    val matchTimeWib: String, // e.g. "22:00 WIB"
    val matchDateWib: String, // e.g. "Sabtu, 15 Nov 2026"
    val matchDateIso: String = "", // e.g. "2026-11-15"
    val liveMinute: String? = null,
    val kickoffEpochMs: Long = 0L,
    val homeColorHex: Long = 0xFF3B82F6,
    val awayColorHex: Long = 0xFFEF4444
) {
    val shortHomeTeam: String
        get() = getClubShortName(homeTeam)

    val shortAwayTeam: String
        get() = getClubShortName(awayTeam)

    companion object {
        fun getClubShortName(name: String): String {
            val lower = name.lowercase().trim()
            return when {
                lower.contains("manchester united") || lower.contains("man united") || lower.contains("man utd") -> "MUN"
                lower.contains("manchester city") || lower.contains("man city") -> "MCI"
                lower.contains("arsenal") -> "ARS"
                lower.contains("chelsea") -> "CHE"
                lower.contains("liverpool") -> "LIV"
                lower.contains("tottenham") || lower.contains("spurs") -> "TOT"
                lower.contains("brentford") -> "BRE"
                lower.contains("fulham") -> "FUL"
                lower.contains("newcastle") -> "NEW"
                lower.contains("aston villa") -> "AVL"
                lower.contains("brighton") -> "BHA"
                lower.contains("wolverhampton") || lower.contains("wolves") -> "WOL"
                lower.contains("everton") -> "EVE"
                lower.contains("west ham") -> "WHU"
                lower.contains("crystal palace") -> "CRY"
                lower.contains("bournemouth") -> "BOU"
                lower.contains("nottingham") -> "NFO"
                lower.contains("leicester") -> "LEI"
                lower.contains("southampton") -> "SOU"
                lower.contains("ipswich") -> "IPS"
                lower.contains("real madrid") -> "RMA"
                lower.contains("barcelona") -> "BAR"
                lower.contains("atlético") || lower.contains("atletico") -> "ATM"
                lower.contains("sevilla") -> "SEV"
                lower.contains("villarreal") -> "VIL"
                lower.contains("real sociedad") -> "RSO"
                lower.contains("athletic") -> "ATH"
                lower.contains("bayern") -> "BAY"
                lower.contains("dortmund") -> "BVB"
                lower.contains("juventus") -> "JUV"
                lower.contains("inter") -> "INT"
                lower.contains("milan") -> "ACM"
                lower.contains("roma") -> "ROM"
                lower.contains("napoli") -> "NAP"
                lower.contains("atalanta") -> "ATA"
                lower.contains("fiorentina") -> "FIO"
                lower.contains("lazio") -> "LAZ"
                lower.contains("paris") || lower.contains("psg") -> "PSG"
                lower.contains("ajax") -> "AJA"
                lower.contains("psv") -> "PSV"
                lower.contains("porto") -> "POR"
                lower.contains("feyenoord") -> "FEY"
                lower.contains("leipzig") -> "RBL"
                lower.contains("stuttgart") -> "VFB"
                lower.contains("lille") -> "LIL"
                lower.contains("sporting") -> "SCP"
                lower.contains("benfica") -> "SLB"
                lower.contains("brugge") -> "BRU"
                lower.contains("galatasaray") -> "GAL"
                lower.contains("fenerbahce") || lower.contains("fenerbahçe") -> "FEN"
                lower.contains("bodo") || lower.contains("bodø") -> "BOD"
                lower.contains("lask") -> "LAS"
                lower.contains("shakhtar") -> "SHK"
                lower.contains("slavia") -> "SLA"
                lower.contains("slovan") -> "SLO"
                lower.contains("betis") -> "BET"
                lower.contains("como") -> "COM"
                lower.contains("lens") -> "LEN"
                lower.contains("aek") -> "AEK"
                lower.contains("sabah") -> "SAB"
                lower.contains("viking") -> "VIK"
                lower.contains("leverkusen") -> "LEV"
                lower.contains("monaco") -> "MON"
                lower.contains("celtic") -> "CEL"
                name.length == 3 -> name.uppercase()
                else -> {
                    val letters = name.filter { it.isLetter() }
                    if (letters.length >= 3) letters.take(3).uppercase()
                    else name.uppercase()
                }
            }
        }
    }
}
