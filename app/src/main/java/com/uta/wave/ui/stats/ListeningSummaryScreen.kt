package com.uta.wave.ui.stats

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.data.local.LocalDatabase
import com.uta.wave.data.local.WeeklyListeningSummary

/**
 * 100% 1:1 Pixel-Accurate Replica of Echo Music's Listening Summary Screen.
 * Contains:
 * 1. "Your Music Vibe This Week" Callout Card with percentage & emoji
 * 2. "Total listened" (5h 5m) with Calendar icon, Weekly 7-Day Pill Bar Chart (M T W T F S S),
 *    Star badge over peak day, and "< This week >" switcher
 * 3. "Listening breakdown" Switch with Top Styles & Artists progression
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningSummaryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localDatabase = remember { LocalDatabase.getInstance(context) }
    val summaryState: WeeklyListeningSummary? by localDatabase.weeklySummaryFlow.collectAsState()
    val summary = summaryState ?: remember { localDatabase.queryWeeklyListeningSummarySync() }

    BackHandler {
        onBack()
    }

    var isBreakdownEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Listening Summary",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF140C16)
                )
            )
        },
        containerColor = Color(0xFF100912),
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // -------------------------------------------------------------
            // CARD 1: "Your Music Vibe This Week"
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF2A1C2C), Color(0xFF1B111E))
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFFF2AEC7).copy(alpha = 0.25f),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFF2AEC7),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Your Music Vibe This Week",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color(0xFFF2AEC7)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val vibeText = buildAnnotatedString {
                        append("❤️ ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                            append(summary.dominantStyle)
                        }
                        append(" is your most-played style this week! Your ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFF2AEC7))) {
                            append(summary.dominantStyle)
                        }
                        append(" listening increased by ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFF2AEC7))) {
                            append("${summary.stylePercentageGrowth}%")
                        }
                        append(" compared with last week.")
                    }

                    Text(
                        text = vibeText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        ),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            // -------------------------------------------------------------
            // CARD 2: Total Listened & 7-Day Pill Bar Chart
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF1E1522))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    // Top Row: "Total listened" + "5h 5m" & Calendar Pill Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total listened",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color(0xFF9E92A2)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = summary.formattedTotalTime,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Calendar",
                                tint = Color(0xFFF2AEC7),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(26.dp))

                    // Weekly 7-Day Bar Chart: M T W T F S S
                    val maxMinutes = summary.days.maxOfOrNull { it.minutesListened }?.coerceAtLeast(1) ?: 1

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        summary.days.forEach { day ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Star badge directly over peak day
                                Box(
                                    modifier = Modifier.size(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (day.isPeakDay) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Peak Day",
                                            tint = Color(0xFFF2AEC7),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Vertical capsule track & filled bar
                                Box(
                                    modifier = Modifier
                                        .width(26.dp)
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(Color(0xFF281E2B)),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    val barRatio = (day.minutesListened.toFloat() / maxMinutes).coerceIn(0f, 1f)
                                    val barHeight = (barRatio * 130).dp.coerceAtLeast(if (day.minutesListened > 0) 14.dp else 0.dp)

                                    if (day.minutesListened > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(barHeight)
                                                .clip(RoundedCornerShape(13.dp))
                                                .background(
                                                    if (day.isPeakDay) Color(0xFFF2AEC7) else Color(0xFF5A3C5C)
                                                )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Day label (M, T, W, T, F, S, S)
                                Text(
                                    text = day.dayLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp,
                                        fontWeight = if (day.isPeakDay) FontWeight.ExtraBold else FontWeight.Medium
                                    ),
                                    color = if (day.isPeakDay) Color(0xFFF2AEC7) else Color(0xFF9E92A2)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Bottom Pill Switcher: "< This week >"
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { /* Week navigation */ }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                                contentDescription = "Previous week",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "This week",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "Next week",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // CARD 3: "Listening breakdown" Switch with Top Styles & Artists
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF1E1522))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Listening breakdown",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "View top genres, vibes and artist stats",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = Color.White.copy(alpha = 0.60f)
                            )
                        }

                        Switch(
                            checked = isBreakdownEnabled,
                            onCheckedChange = { isBreakdownEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFF2AEC7),
                                uncheckedThumbColor = Color(0xFF8E7D8A),
                                uncheckedTrackColor = Color(0xFF2C2230)
                            )
                        )
                    }

                    AnimatedVisibility(
                        visible = isBreakdownEnabled,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(top = 18.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "TOP STYLES",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    fontSize = 11.sp
                                ),
                                color = Color(0xFFF2AEC7)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            summary.topStyles.forEach { (style, percent) ->
                                Column(modifier = Modifier.padding(vertical = 5.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = style,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = Color.White
                                        )
                                        Text(
                                            text = "$percent%",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFF2AEC7)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { percent / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = Color(0xFFF2AEC7),
                                        trackColor = Color.White.copy(alpha = 0.08f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = "TOP ARTISTS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    fontSize = 11.sp
                                ),
                                color = Color(0xFFF2AEC7)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            summary.topArtists.forEachIndexed { idx, (artist, percent) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#${idx + 1}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = Color(0xFFF2AEC7),
                                        modifier = Modifier.width(28.dp)
                                    )
                                    Text(
                                        text = artist,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "$percent% replay",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = Color.White.copy(alpha = 0.60f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
