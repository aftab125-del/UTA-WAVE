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
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.uta.wave.data.preferences.UserPreferences
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.player.WavePlayerState
import com.uta.wave.ui.components.GlassComponent
import com.uta.wave.ui.components.LocalGlassEffectConfig
import com.uta.wave.ui.components.liquidGlass
import com.uta.wave.ui.recognizer.BlushRose
import com.uta.wave.ui.recognizer.DarkBlushText
import com.uta.wave.ui.theme.ElectricViolet
import com.uta.wave.ui.theme.SoftViolet
import com.uta.wave.ui.theme.TextMuted
import com.uta.wave.ui.theme.TextPrimary

// Glassmorphism Gradients & Highlights (Dynamic theme reactive)
var GlassBackgroundBrush by mutableStateOf(
    Brush.verticalGradient(
        listOf(Color(0xD922182B), Color(0xBF140D1C))
    )
)

var GlassBorderBrush by mutableStateOf(
    Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.05f))
    )
)

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
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences.getInstance(context) }
    val settings by userPrefs.settingsFlow.collectAsState()
    val isPureBlack = settings.pureBlack

    val glassConfig = LocalGlassEffectConfig.current
    val useGlass = glassConfig.isEnabledFor(GlassComponent.NAV_BAR)
    val miniPlayerBackground = settings.miniplayerBackgroundStyle
    val isGlassMini = (miniPlayerBackground == "LIQUID_GLASS" || glassConfig.isEnabledFor(GlassComponent.MINI_PLAYER)) && isGlassSupported() && !isPureBlack
    val isDynamicMini = !isGlassMini && miniPlayerBackground in listOf("BLUR", "GLOW_ANIMATED", "LIVE_MESH", "ACRYLIC_BLUR")

    val adaptiveTextColor =
        if (glassConfig.textColor.isSpecified) {
            glassConfig.textColor
        } else if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
            Color.Black
        } else {
            Color.White
        }

    val selectedContentColor = if (useGlass) adaptiveTextColor else BlushRose
    val unselectedContentColor = if (useGlass) adaptiveTextColor.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.7f)

    val tabs = listOf(
        NavTab(0, "Home", Icons.Filled.Home, Icons.Outlined.Home),
        NavTab(1, "Radio", Icons.Filled.Radio, Icons.Outlined.Radio),
        NavTab(2, "Library", Icons.AutoMirrored.Filled.QueueMusic, Icons.AutoMirrored.Outlined.QueueMusic)
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
                        2 -> Icons.AutoMirrored.Filled.QueueMusic
                        3 -> Icons.Filled.Search
                        else -> Icons.Filled.Home
                    }

                    val activeFabModifier = if (useGlass) {
                        Modifier
                            .size(50.dp)
                            .liquidGlass(config = glassConfig, shape = CircleShape)
                            .clickable(onClick = onExpandBar)
                    } else {
                        Modifier
                            .size(50.dp)
                            .shadow(14.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.5f))
                            .clip(CircleShape)
                            .background(GlassBackgroundBrush)
                            .border(1.dp, GlassBorderBrush, CircleShape)
                            .clickable(onClick = onExpandBar)
                    }

                    Box(
                        modifier = activeFabModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = activeIcon,
                            contentDescription = "Active Tab",
                            tint = selectedContentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // 2. Center Slim MiniPlayer Capsule Pill
                    val collapsedMiniModifier = if (useGlass || isGlassMini) {
                        Modifier
                            .weight(1f)
                            .height(50.dp)
                            .liquidGlass(config = glassConfig, shape = RoundedCornerShape(percent = 50))
                            .clickable(onClick = onExpandPlayer)
                    } else {
                        Modifier
                            .weight(1f)
                            .height(50.dp)
                            .shadow(16.dp, RoundedCornerShape(25.dp), spotColor = Color.Black.copy(alpha = 0.6f))
                            .clip(RoundedCornerShape(25.dp))
                            .background(
                                if (isDynamicMini) {
                                    Brush.linearGradient(listOf(Color(0xFF0C0911), Color(0xFF0C0911)))
                                } else {
                                    Brush.verticalGradient(
                                        listOf(Color(0xD9281C30), Color(0xBF181120))
                                    )
                                }
                            )
                            .border(
                                1.dp,
                                if (isDynamicMini) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.10f))) else GlassBorderBrush,
                                RoundedCornerShape(25.dp)
                            )
                            .clickable(onClick = onExpandPlayer)
                    }

                    val miniTitleColor = when {
                        useGlass || isGlassMini -> adaptiveTextColor
                        isDynamicMini -> Color.White
                        else -> TextPrimary
                    }

                    val miniSubtitleColor = when {
                        useGlass || isGlassMini -> adaptiveTextColor.copy(alpha = 0.7f)
                        isDynamicMini -> Color.White.copy(alpha = 0.75f)
                        else -> SoftViolet
                    }

                    val miniAccentColor = when {
                        useGlass || isGlassMini -> adaptiveTextColor
                        isDynamicMini -> Color.White
                        else -> selectedContentColor
                    }

                    Box(
                        modifier = collapsedMiniModifier
                    ) {
                        if (isDynamicMini) {
                            MiniPlayerBackgroundLayer(
                                style = miniPlayerBackground,
                                artworkUrl = currentTrack.artworkUrl,
                                modifier = Modifier.matchParentSize(),
                                shape = RoundedCornerShape(25.dp)
                            )
                        }

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
                                        .border(1.dp, miniAccentColor.copy(alpha = 0.6f), CircleShape),
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
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1B1522))
                                            .border(0.5.dp, miniAccentColor, CircleShape)
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
                                        color = miniTitleColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = currentTrack.artist,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = miniSubtitleColor,
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
                                        .background(miniAccentColor.copy(alpha = if (useGlass || isGlassMini || isDynamicMini) 0.15f else 0.2f))
                                        .clickable(onClick = onTogglePlayPause),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        playerState.isResolving || playerState.isBuffering -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = miniAccentColor,
                                                strokeWidth = 2.dp
                                            )
                                        }
                                        playerState.isPlaying -> {
                                            Icon(
                                                imageVector = Icons.Default.Pause,
                                                contentDescription = "Pause",
                                                tint = miniAccentColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        else -> {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = miniAccentColor,
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
                                            if (useGlass || isGlassMini) {
                                                Brush.horizontalGradient(
                                                    listOf(adaptiveTextColor, adaptiveTextColor.copy(alpha = 0.35f))
                                                )
                                            } else if (isDynamicMini) {
                                                Brush.horizontalGradient(
                                                    listOf(Color.White, Color.White.copy(alpha = 0.45f))
                                                )
                                            } else {
                                                Brush.horizontalGradient(
                                                    listOf(BlushRose, ElectricViolet)
                                                )
                                            }
                                        )
                                )
                            }
                        }
                    }

                    // 3. Right Search Circular FAB
                    val searchFabModifier = if (useGlass) {
                        Modifier
                            .size(50.dp)
                            .liquidGlass(config = glassConfig, shape = CircleShape)
                            .clickable { onTabSelected(3) }
                    } else {
                        Modifier
                            .size(50.dp)
                            .shadow(14.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.5f))
                            .clip(CircleShape)
                            .background(GlassBackgroundBrush)
                            .border(1.dp, GlassBorderBrush, CircleShape)
                            .clickable { onTabSelected(3) }
                    }

                    Box(
                        modifier = searchFabModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (selectedTab == 3) Icons.Filled.Search else Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = if (selectedTab == 3) selectedContentColor else unselectedContentColor,
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
                    val tabsCapsuleModifier = if (useGlass) {
                        Modifier
                            .height(54.dp)
                            .liquidGlass(config = glassConfig, shape = RoundedCornerShape(percent = 50))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    } else {
                        Modifier
                            .height(54.dp)
                            .shadow(18.dp, RoundedCornerShape(28.dp), spotColor = Color.Black.copy(alpha = 0.6f))
                            .clip(RoundedCornerShape(28.dp))
                            .background(GlassBackgroundBrush)
                            .border(1.dp, GlassBorderBrush, RoundedCornerShape(28.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    }

                    Row(
                        modifier = tabsCapsuleModifier,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEach { tab ->
                            val isSelected = selectedTab == tab.index
                            val interactionSource = remember { MutableInteractionSource() }

                            val pillBackground by animateColorAsState(
                                targetValue = if (isSelected) {
                                    if (useGlass) adaptiveTextColor.copy(alpha = 0.22f) else BlushRose
                                } else Color.Transparent,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "tabPillBackground"
                            )

                            val iconTint by animateColorAsState(
                                targetValue = if (isSelected) {
                                    if (useGlass) adaptiveTextColor else DarkBlushText
                                } else unselectedContentColor,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "tabIconTint"
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
                                            color = if (useGlass) adaptiveTextColor else DarkBlushText,
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
                    val expandedSearchModifier = if (useGlass) {
                        Modifier
                            .size(54.dp)
                            .liquidGlass(config = glassConfig, shape = CircleShape)
                            .clickable { onTabSelected(3) }
                    } else {
                        Modifier
                            .size(54.dp)
                            .shadow(18.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.6f))
                            .clip(CircleShape)
                            .background(GlassBackgroundBrush)
                            .border(1.dp, GlassBorderBrush, CircleShape)
                            .clickable { onTabSelected(3) }
                    }

                    Box(
                        modifier = expandedSearchModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSearchSelected) Icons.Filled.Search else Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = if (isSearchSelected) selectedContentColor else unselectedContentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
