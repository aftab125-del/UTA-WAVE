package com.uta.wave.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.player.WavePlayerState
import com.uta.wave.ui.theme.CardBorder
import com.uta.wave.ui.theme.ElectricViolet
import com.uta.wave.ui.theme.SoftViolet
import com.uta.wave.ui.theme.TextMuted
import com.uta.wave.ui.theme.TextPrimary
import com.uta.wave.ui.theme.TextSecondary

data class NavTab(
    val index: Int,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun FloatingNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    isCollapsed: Boolean,
    onExpandBar: () -> Unit,
    currentTrack: TrackItem?,
    playerState: WavePlayerState,
    onExpandPlayer: () -> Unit,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        NavTab(0, "Home", Icons.Filled.Home, Icons.Outlined.Home),
        NavTab(1, "Radio", Icons.Filled.Radio, Icons.Outlined.Radio),
        NavTab(2, "Library", Icons.Filled.QueueMusic, Icons.Outlined.QueueMusic)
    )

    // Infinite rotation for spinning vinyl disk
    val infiniteTransition = rememberInfiniteTransition(label = "VinylSpinCollapsed")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "DiskRotationCollapsed"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        val shouldShowMerged = isCollapsed && currentTrack != null

        AnimatedContent(
            targetState = shouldShowMerged,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
            },
            label = "BottomBarModeAnimation"
        ) { merged ->
            if (merged && currentTrack != null) {
                // COLLAPSED MERGED MODE: [Active Tab Circle] --- [Slim MiniPlayer Pill] --- [Search Circle]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Left Active Tab Circular FAB
                    val activeIcon = when (selectedTab) {
                        1 -> Icons.Filled.Radio
                        2 -> Icons.Filled.QueueMusic
                        3 -> Icons.Filled.Search
                        else -> Icons.Filled.Home
                    }

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .shadow(12.dp, CircleShape, spotColor = ElectricViolet.copy(alpha = 0.3f))
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF271F2E), Color(0xFF1B1522))
                                )
                            )
                            .border(1.dp, ElectricViolet.copy(alpha = 0.5f), CircleShape)
                            .clickable(onClick = onExpandBar),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = activeIcon,
                            contentDescription = "Active Tab",
                            tint = ElectricViolet,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // 2. Center Slim MiniPlayer Capsule Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .shadow(12.dp, RoundedCornerShape(25.dp), spotColor = ElectricViolet.copy(alpha = 0.25f))
                            .clip(RoundedCornerShape(25.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF2C2233), Color(0xFF1E1724))
                                )
                            )
                            .border(1.dp, CardBorder.copy(alpha = 0.8f), RoundedCornerShape(25.dp))
                            .clickable(onClick = onExpandPlayer)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Mini Vinyl Disc
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, ElectricViolet.copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = currentTrack.artworkUrl,
                                        contentDescription = currentTrack.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .rotate(if (playerState.isPlaying) rotation else 0f)
                                    )
                                    // Spindle center dot
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1B1522))
                                            .border(0.5.dp, ElectricViolet, CircleShape)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Song title & artist
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentTrack.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        ),
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = currentTrack.artist,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = SoftViolet,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Play / Pause Button
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(ElectricViolet.copy(alpha = 0.2f))
                                        .clickable(onClick = onTogglePlayPause),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        playerState.isResolving || playerState.isBuffering -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = ElectricViolet,
                                                strokeWidth = 2.dp
                                            )
                                        }
                                        playerState.isPlaying -> {
                                            Icon(
                                                imageVector = Icons.Default.Pause,
                                                contentDescription = "Pause",
                                                tint = ElectricViolet,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        else -> {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = ElectricViolet,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Thin progress line at bottom of pill
                            val progress = if (playerState.durationMs > 0) {
                                (playerState.positionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
                            } else 0f

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = progress)
                                        .height(2.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(ElectricViolet, Color(0xFFFF75B5))
                                            )
                                        )
                                )
                            }
                        }
                    }

                    // 3. Right Search Circular FAB
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .shadow(12.dp, CircleShape, spotColor = ElectricViolet.copy(alpha = 0.25f))
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF271F2E), Color(0xFF1B1522))
                                )
                            )
                            .border(1.dp, CardBorder.copy(alpha = 0.6f), CircleShape)
                            .clickable { onTabSelected(3) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (selectedTab == 3) Icons.Filled.Search else Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = if (selectedTab == 3) ElectricViolet else TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                // EXPANDED MODE: [Left Capsule Pill: Home, Radio, Library] + [Right Circle: Search]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Capsule Pill
                    Row(
                        modifier = Modifier
                            .height(54.dp)
                            .shadow(16.dp, RoundedCornerShape(28.dp), spotColor = ElectricViolet.copy(alpha = 0.25f))
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF271F2E), Color(0xFF1B1522))
                                )
                            )
                            .border(1.dp, CardBorder.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEach { tab ->
                            val isSelected = selectedTab == tab.index
                            val interactionSource = remember { MutableInteractionSource() }

                            val iconTint by animateColorAsState(
                                targetValue = if (isSelected) ElectricViolet else TextMuted,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "tabIconTint"
                            )

                            val pillBackground by animateColorAsState(
                                targetValue = if (isSelected) ElectricViolet.copy(alpha = 0.18f) else Color.Transparent,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "tabPillBackground"
                            )

                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(pillBackground)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) { onTabSelected(tab.index) }
                                    .padding(horizontal = if (isSelected) 14.dp else 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title,
                                        tint = iconTint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    if (isSelected) {
                                        Text(
                                            text = "  ${tab.title}",
                                            color = TextPrimary,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Right Standalone Circular Search Button
                    val isSearchSelected = selectedTab == 3
                    val searchBorderColor by animateColorAsState(
                        targetValue = if (isSearchSelected) ElectricViolet else CardBorder.copy(alpha = 0.6f),
                        label = "searchBorder"
                    )

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .shadow(16.dp, CircleShape, spotColor = ElectricViolet.copy(alpha = 0.25f))
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF271F2E), Color(0xFF1B1522))
                                )
                            )
                            .border(1.dp, searchBorderColor, CircleShape)
                            .clickable { onTabSelected(3) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSearchSelected) Icons.Filled.Search else Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = if (isSearchSelected) ElectricViolet else TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
