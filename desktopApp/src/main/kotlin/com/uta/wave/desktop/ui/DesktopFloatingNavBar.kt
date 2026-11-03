package com.uta.wave.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.desktop.DesktopNavTab

data class FloatingTabItem(
    val tab: DesktopNavTab,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val MainFloatingTabs = listOf(
    FloatingTabItem(DesktopNavTab.EXPLORE, Icons.Filled.Explore, Icons.Outlined.Explore),
    FloatingTabItem(DesktopNavTab.SEARCH, Icons.Filled.Search, Icons.Outlined.Search),
    FloatingTabItem(DesktopNavTab.LIBRARY, Icons.AutoMirrored.Filled.QueueMusic, Icons.AutoMirrored.Outlined.QueueMusic),
    FloatingTabItem(DesktopNavTab.CONNECT, Icons.Filled.Cast, Icons.Outlined.Cast),
    FloatingTabItem(DesktopNavTab.SETTINGS, Icons.Filled.Settings, Icons.Outlined.Settings)
)

/**
 * UTA-WAVE Liquid Glass Floating Navigation Bar for Windows Desktop.
 * Directly adapts the signature mobile floating pill with spring-physics active pill,
 * specular edge reflections, and companion glass action button.
 */
@Composable
fun DesktopFloatingNavBar(
    selectedTab: DesktopNavTab,
    onTabSelected: (DesktopNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Center Floating Liquid Glass Capsule Pill
        Box(
            modifier = Modifier
                .desktopLiquidGlass(
                    shape = RoundedCornerShape(percent = 50),
                    elevation = 20.dp
                )
                .height(56.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MainFloatingTabs.forEach { tabItem ->
                    val isSelected = selectedTab == tabItem.tab
                    val interactionSource = remember { MutableInteractionSource() }
                    val isHovered by interactionSource.collectIsHoveredAsState()

                    // Smooth spring animation for active pill background
                    val pillBackground by animateColorAsState(
                        targetValue = when {
                            isSelected -> GlassActivePill
                            isHovered -> Color.White.copy(alpha = 0.08f)
                            else -> Color.Transparent
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "pillBackground"
                    )

                    // Smooth spring animation for icon and text tint
                    val contentColor by animateColorAsState(
                        targetValue = when {
                            isSelected -> Color.White
                            isHovered -> Color.White.copy(alpha = 0.9f)
                            else -> GlassTextMuted
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "contentColor"
                    )

                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(pillBackground)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                onTabSelected(tabItem.tab)
                            }
                            .padding(horizontal = if (isSelected) 16.dp else 14.dp),
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
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 2. Right Standalone Circular Liquid Glass Action Button (AI Studio)
        val isAiStudio = selectedTab == DesktopNavTab.AI_STUDIO
        val aiInteractionSource = remember { MutableInteractionSource() }
        val isAiHovered by aiInteractionSource.collectIsHoveredAsState()

        val aiBorderBrush = if (isAiStudio) {
            Brush.sweepGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF8B5CF6)))
        } else {
            DesktopGlassBorderBrush
        }

        val aiBgBrush = if (isAiStudio) {
            Brush.verticalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)))
        } else if (isAiHovered) {
            DesktopGlassHoverBrush
        } else {
            DesktopGlassBackgroundBrush
        }

        Box(
            modifier = Modifier
                .size(56.dp)
                .desktopLiquidGlass(
                    shape = CircleShape,
                    elevation = 20.dp,
                    backgroundBrush = aiBgBrush,
                    borderBrush = aiBorderBrush
                )
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(
                    interactionSource = aiInteractionSource,
                    indication = null
                ) {
                    onTabSelected(DesktopNavTab.AI_STUDIO)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isAiStudio) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                contentDescription = "AI Studio",
                tint = if (isAiStudio) Color.White else if (isAiHovered) Color.White else Color(0xFFC084FC),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
