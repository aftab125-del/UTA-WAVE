package com.uta.wave.desktop.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.audio.UnifiedPlaybackState
import com.uta.wave.core.model.TrackItem
import com.uta.wave.desktop.DesktopNavTab

data class FloatingTabItem(
    val tab: DesktopNavTab,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val MainFloatingTabs = listOf(
    FloatingTabItem(DesktopNavTab.HOME, Icons.Filled.Home, Icons.Outlined.Home),
    FloatingTabItem(DesktopNavTab.LIBRARY, Icons.AutoMirrored.Filled.QueueMusic, Icons.AutoMirrored.Outlined.QueueMusic),
    FloatingTabItem(DesktopNavTab.SETTINGS, Icons.Filled.Settings, Icons.Outlined.Settings)
)

/**
 * Authentic UTA-WAVE Dual-Mode Liquid Glass Floating Navigation Bar for Windows Desktop.
 *
 * Direct parity with the mobile app's scroll-adjustable FloatingNavBar:
 * - EXPANDED MODE: Full navigation tabs pill capsule + companion AI Studio FAB.
 * - COLLAPSED MERGED MODE (on scroll-down): Transitions seamlessly into:
 *   [Active Tab Circle (50dp)] ──── [Slim Inline MiniPlayer (50dp)] ──── [AI Studio FAB (50dp)]
 */
@Composable
fun DesktopFloatingNavBar(
    selectedTab: DesktopNavTab,
    onTabSelected: (DesktopNavTab) -> Unit,
    isCollapsed: Boolean = false,
    onExpandBar: () -> Unit = {},
    currentTrack: TrackItem? = null,
    playbackState: UnifiedPlaybackState = UnifiedPlaybackState(),
    onTogglePlayPause: () -> Unit = {},
    onExpandPlayer: () -> Unit = {},
    backdrop: DesktopBackdrop? = null,
    modifier: Modifier = Modifier
) {
    // Vinyl Disc 360-degree rotation for inline collapsed mode
    val infiniteTransition = rememberInfiniteTransition(label = "InlineVinylTransition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "InlineVinylRotation"
    )

    val currentRotation = if (playbackState.isPlaying) rotation else 0f
    val hasTrack = currentTrack != null || !playbackState.videoId.isNullOrBlank()

    AnimatedContent(
        targetState = isCollapsed && hasTrack,
        transitionSpec = {
            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
        },
        label = "DesktopBottomBarModeAnimation",
        modifier = modifier.wrapContentWidth()
    ) { merged ->
        if (merged) {
            // =================================================================
            // COLLAPSED MERGED MODE: [Active Tab Circle] ── [Inline MiniPlayer] ── [AI Studio Circle]
            // =================================================================
            Row(
                modifier = Modifier.width(620.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Left Active Tab Circular FAB (Clicking expands full bar)
                val activeTabItem = MainFloatingTabs.firstOrNull { it.tab == selectedTab }
                    ?: MainFloatingTabs[0]

                val tabInteractionSource = remember { MutableInteractionSource() }
                val isTabHovered by tabInteractionSource.collectIsHoveredAsState()

                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .desktopLiquidGlass(
                            backdrop = backdrop,
                            shape = CircleShape,
                            blurRadius = 36f,
                            elevation = if (isTabHovered) 28.dp else 22.dp
                        )
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(
                            interactionSource = tabInteractionSource,
                            indication = null,
                            onClick = onExpandBar
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = activeTabItem.selectedIcon,
                        contentDescription = "Expand Navigation Bar",
                        tint = if (isTabHovered) Color.White else Color(0xFFC084FC),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 2. Center Slim MiniPlayer Capsule Pill
                val inlineInteraction = remember { MutableInteractionSource() }
                val isInlineHovered by inlineInteraction.collectIsHoveredAsState()

                val progress = if (playbackState.durationMs > 0) {
                    (playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .desktopLiquidGlass(
                            backdrop = backdrop,
                            shape = RoundedCornerShape(percent = 50),
                            blurRadius = 36f,
                            elevation = if (isInlineHovered) 28.dp else 22.dp
                        )
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(
                            interactionSource = inlineInteraction,
                            indication = null,
                            onClick = onExpandPlayer
                        )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mini Vinyl Disc Artwork
                            val artUrl = playbackState.artworkUrl ?: currentTrack?.artworkUrl
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!artUrl.isNullOrBlank()) {
                                    DesktopNetworkImage(
                                        url = artUrl,
                                        contentDescription = playbackState.title,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .rotate(currentRotation),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF1E1728)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.MusicNote,
                                            contentDescription = null,
                                            tint = Color(0xFFC084FC),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                // Center hole
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1B1522))
                                        .border(0.5.dp, Color(0xFF8B5CF6), CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Track Title & Artist in Compact Single/Double Line
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = playbackState.title.ifBlank { currentTrack?.title ?: "Playing" },
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = playbackState.artist.ifBlank { currentTrack?.artist ?: "UTA-WAVE" },
                                    color = Color(0xFFC084FC),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Play / Pause Circle Button
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.20f))
                                    .border(0.75.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                                    .clickable(onClick = onTogglePlayPause),
                                contentAlignment = Alignment.Center
                            ) {
                                if (playbackState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else if (playbackState.isPlaying) {
                                    Icon(
                                        imageVector = Icons.Filled.Pause,
                                        contentDescription = "Pause",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Thin progress line along bottom edge
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
                                            listOf(
                                                Color(0xFF8B5CF6),
                                                Color(0xFFEC4899),
                                                Color(0xFF38BDF8)
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }

                // 3. Right Companion Circular FAB (Search)
                val isSearch = selectedTab == DesktopNavTab.SEARCH
                val searchInteractionSource = remember { MutableInteractionSource() }
                val isSearchHovered by searchInteractionSource.collectIsHoveredAsState()

                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .desktopLiquidGlass(
                            backdrop = backdrop,
                            shape = CircleShape,
                            blurRadius = 36f,
                            elevation = if (isSearchHovered) 28.dp else 22.dp
                        )
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(
                            interactionSource = searchInteractionSource,
                            indication = null,
                            onClick = { onTabSelected(DesktopNavTab.SEARCH) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSearch) Icons.Filled.Search else Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = if (isSearch || isSearchHovered) Color.White else Color(0xFFC084FC),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        } else {
            // =================================================================
            // EXPANDED MODE: [Floating Tabs Pill Capsule] ── [AI Studio Circle]
            // =================================================================
            Row(
                modifier = Modifier.wrapContentWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Center Floating Liquid Glass Capsule Pill (Tabs)
                Box(
                    modifier = Modifier
                        .height(54.dp)
                        .desktopLiquidGlass(
                            backdrop = backdrop,
                            shape = RoundedCornerShape(percent = 50),
                            blurRadius = 36f,
                            elevation = 24.dp
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MainFloatingTabs.forEach { tabItem ->
                            val isSelected = selectedTab == tabItem.tab
                            val interactionSource = remember { MutableInteractionSource() }
                            val isHovered by interactionSource.collectIsHoveredAsState()

                            // Authentic mobile frosted white glass pill for the active tab
                            val pillBackground by animateColorAsState(
                                targetValue = when {
                                    isSelected -> Color.White.copy(alpha = 0.22f)
                                    isHovered -> Color.White.copy(alpha = 0.08f)
                                    else -> Color.Transparent
                                },
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "pillBackground"
                            )

                            val contentColor by animateColorAsState(
                                targetValue = when {
                                    isSelected -> Color.White
                                    isHovered -> Color.White.copy(alpha = 0.95f)
                                    else -> Color.White.copy(alpha = 0.65f)
                                },
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "contentColor"
                            )

                            val pillModifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(pillBackground)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 0.75.dp,
                                            brush = GlassActivePillBorderBrush,
                                            shape = RoundedCornerShape(22.dp)
                                        )
                                    } else Modifier
                                )
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    onTabSelected(tabItem.tab)
                                }
                                .padding(horizontal = if (isSelected) 14.dp else 12.dp)

                            Box(
                                modifier = pillModifier,
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) tabItem.selectedIcon else tabItem.unselectedIcon,
                                        contentDescription = tabItem.tab.label,
                                        tint = contentColor,
                                        modifier = Modifier.size(20.dp)
                                    )

                                    // Smooth animated label reveal for active tab
                                    AnimatedVisibility(
                                        visible = isSelected,
                                        enter = expandHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                                        exit = shrinkHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                                    ) {
                                        Text(
                                            text = "  ${tabItem.tab.label}",
                                            color = Color.White,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // 2. Right Standalone Circular Liquid Glass Action Button (Search)
                val isSearch = selectedTab == DesktopNavTab.SEARCH
                val searchInteractionSource = remember { MutableInteractionSource() }
                val isSearchHovered by searchInteractionSource.collectIsHoveredAsState()

                val searchBorderBrush = if (isSearch) {
                    Brush.sweepGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF8B5CF6)))
                } else {
                    GlassSpecularBorderBrush
                }

                val searchIconTint by animateColorAsState(
                    targetValue = when {
                        isSearch -> Color.White
                        isSearchHovered -> Color.White
                        else -> Color(0xFFC084FC)
                    },
                    label = "searchIconTint"
                )

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .desktopLiquidGlass(
                            backdrop = backdrop,
                            shape = CircleShape,
                            blurRadius = 36f,
                            borderBrush = searchBorderBrush,
                            elevation = 24.dp
                        )
                        .then(
                            if (isSearch) {
                                Modifier.background(
                                    Brush.radialGradient(
                                        listOf(Color(0x558B5CF6), Color.Transparent)
                                    ),
                                    CircleShape
                                )
                            } else Modifier
                        )
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(
                            interactionSource = searchInteractionSource,
                            indication = null
                        ) {
                            onTabSelected(DesktopNavTab.SEARCH)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSearch) Icons.Filled.Search else Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = searchIconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
