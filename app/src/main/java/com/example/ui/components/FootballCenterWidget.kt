package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.LeagueScheduleResult
import com.example.model.FootballMatch
import com.example.model.LeagueInfo
import com.example.model.MatchStatus
import com.example.ui.theme.TvFocusPurpleLight
import com.example.ui.theme.tvFocusable

/**
 * Football Center Widget (Android TV)
 * - Frosted Glass & Liquid Glass aesthetic (~70% transparency, soft blur, translucent white border)
 * - Slim vertical footprint on the left side of the main screen
 * - Exclusively features the 4 core competitions:
 *     1. UEFA Champions League
 *     2. Premier League
 *     3. Serie A
 *     4. La Liga
 * - Initial state: Displays league names and badges without match lists
 * - Interactive: Pressing D-pad OK opens that league's schedule panel with same-day matches
 * - Back button cleanly dismisses schedule panel
 */
@Composable
fun FootballCenterWidget(
    leagues: List<LeagueInfo>,
    selectedLeague: LeagueInfo?,
    scheduleResult: LeagueScheduleResult?,
    isLoadingSchedule: Boolean,
    onSelectLeague: (LeagueInfo) -> Unit,
    onBackToLeagues: () -> Unit,
    onMatchClick: (FootballMatch) -> Unit,
    modifier: Modifier = Modifier
) {
    // Intercept remote Back key when schedule panel is active
    BackHandler(enabled = selectedLeague != null) {
        onBackToLeagues()
    }

    Row(
        modifier = modifier.fillMaxHeight(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // 1. Slim Vertical Frosted Glass Sidebar (The 4 Competitions)
        Box(
            modifier = Modifier
                .width(236.dp)
                .clip(RoundedCornerShape(20.dp))
                // Frosted Glass (~70% opacity dark liquid tint)
                .background(Color(0xB30F172A))
                // Translucent white border with liquid gradient
                .border(1.2.dp, Color(0x38FFFFFF), RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            // Liquid glass specular sheen overlay at top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0x22FFFFFF), Color.Transparent)
                        )
                    )
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SportsSoccer,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FOOTBALL CENTER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Text(
                        text = "4 Liga",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFA855F7)
                    )
                }

                Text(
                    text = "Pilih kompetisi dengan OK remote",
                    fontSize = 9.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // The 4 Competitions Cards
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    leagues.forEach { league ->
                        val isSelected = selectedLeague?.code == league.code
                        LeagueItemCard(
                            league = league,
                            isSelected = isSelected,
                            onClick = { onSelectLeague(league) }
                        )
                    }
                }
            }
        }

        // 2. Interactive Schedule Panel (Opens when a league is clicked)
        AnimatedVisibility(
            visible = selectedLeague != null,
            enter = fadeIn(tween(220)) + slideInHorizontally(tween(220)) { -it / 3 },
            exit = fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { -it / 3 }
        ) {
            if (selectedLeague != null) {
                Spacer(modifier = Modifier.width(12.dp))
                LeagueSchedulePanel(
                    league = selectedLeague,
                    scheduleResult = scheduleResult,
                    isLoading = isLoadingSchedule,
                    onBack = onBackToLeagues,
                    onMatchClick = onMatchClick
                )
            }
        }
    }
}

/**
 * Individual Frosted League Card in the Vertical Sidebar
 */
@Composable
private fun LeagueItemCard(
    league: LeagueInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) Color(0x553B82F6) else Color(0x351E293B)
            )
            .border(
                1.dp,
                if (isSelected) Color(0x88A855F7) else Color(0x20FFFFFF),
                RoundedCornerShape(14.dp)
            )
            .tvFocusable(
                accentColor = TvFocusPurpleLight,
                shape = RoundedCornerShape(14.dp),
                scaleOnFocus = 1.06f,
                elevationOnFocus = 10.dp,
                isSelected = isSelected,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Official League Logo
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x30FFFFFF))
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = league.logoUrl,
                        contentDescription = league.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = league.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = league.country,
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = if (isSelected) Color(0xFFC084FC) else Color(0x60FFFFFF),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Schedule Panel: Displays Same-Day Matches sorted by nearest kickoff time
 */
@Composable
private fun LeagueSchedulePanel(
    league: LeagueInfo,
    scheduleResult: LeagueScheduleResult?,
    isLoading: Boolean,
    onBack: () -> Unit,
    onMatchClick: (FootballMatch) -> Unit
) {
    val matches = scheduleResult?.matches ?: emptyList()
    val liveCount = matches.count { it.status == MatchStatus.LIVE }

    val infiniteTransition = rememberInfiniteTransition(label = "panel_live_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "panel_pulse_alpha"
    )

    Box(
        modifier = Modifier
            .width(420.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xC00B132B))
            .border(1.2.dp, Color(0x40FFFFFF), RoundedCornerShape(20.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Panel Header: Back Action + League Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button (Remote focusable)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x351F2937))
                        .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(10.dp))
                        .tvFocusable(
                            accentColor = TvFocusPurpleLight,
                            shape = RoundedCornerShape(10.dp),
                            scaleOnFocus = 1.08f,
                            onClick = onBack
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Kembali (Back)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                // League Banner
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = league.logoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = league.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date & Kickoff Subheader
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x401E293B))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFFA855F7),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = scheduleResult?.dateTitle ?: "JADWAL PERTANDINGAN (WIB)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF1F5F9),
                        letterSpacing = 0.5.sp
                    )
                }

                if (liveCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFEF4444).copy(alpha = pulseAlpha))
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = "$liveCount LIVE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                } else {
                    Text(
                        text = "${matches.size} Laga",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Matches Content
            if (isLoading && matches.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Color(0xFFA855F7),
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Memeriksa jadwal pertandingan...",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            } else if (matches.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada pertandingan terdekat",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(matches) { match ->
                        MatchScheduleCard(
                            match = match,
                            pulseAlpha = pulseAlpha,
                            onClick = { onMatchClick(match) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual Match Row in the Schedule Panel
 * Shows real club crests, names, WIB kickoff time, and real LIVE scores
 */
@Composable
private fun MatchScheduleCard(
    match: FootballMatch,
    pulseAlpha: Float,
    onClick: () -> Unit
) {
    val isLive = match.status == MatchStatus.LIVE

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x301E293B))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
            .tvFocusable(
                accentColor = TvFocusPurpleLight,
                shape = RoundedCornerShape(12.dp),
                scaleOnFocus = 1.04f,
                elevationOnFocus = 8.dp,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home Club (Right aligned)
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = match.homeTeam,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                ClubCrest(
                    logoUrl = match.homeTeamLogoUrl,
                    clubName = match.homeTeam,
                    colorHex = match.homeColorHex
                )
            }

            // Center Match Status / Kickoff Time / Score
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLive) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${match.homeScore ?: 0} - ${match.awayScore ?: 0}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFEF4444)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFEF4444).copy(alpha = pulseAlpha))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "LIVE ${match.liveMinute ?: ""}",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else if (match.status == MatchStatus.FINISHED && match.homeScore != null && match.awayScore != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${match.homeScore} - ${match.awayScore}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = "SELESAI",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8)
                        )
                    }
                } else {
                    // Scheduled Upcoming Match
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = match.matchTimeWib.substringBefore(" "),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "WIB",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // Away Club (Left aligned)
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                ClubCrest(
                    logoUrl = match.awayTeamLogoUrl,
                    clubName = match.awayTeam,
                    colorHex = match.awayColorHex
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = match.awayTeam,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Official Club Crest with fallback to colored badge
 */
@Composable
private fun ClubCrest(
    logoUrl: String?,
    clubName: String,
    colorHex: Long
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color(0x25FFFFFF))
            .padding(1.5.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!logoUrl.isNullOrEmpty()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = clubName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(CircleShape)
                    .background(Color(colorHex)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = clubName.take(1).uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
