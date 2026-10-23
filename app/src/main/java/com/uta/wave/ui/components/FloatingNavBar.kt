package com.uta.wave.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uta.wave.ui.theme.CardBorder
import com.uta.wave.ui.theme.ElectricViolet
import com.uta.wave.ui.theme.SurfaceDark
import com.uta.wave.ui.theme.SurfaceElevated
import com.uta.wave.ui.theme.TextMuted
import com.uta.wave.ui.theme.TextPrimary

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
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        NavTab(0, "Home", Icons.Filled.Home, Icons.Outlined.Home),
        NavTab(1, "Charts", Icons.Filled.TrendingUp, Icons.Outlined.TrendingUp),
        NavTab(2, "Library", Icons.Filled.QueueMusic, Icons.Outlined.QueueMusic),
        NavTab(3, "Search", Icons.Filled.Search, Icons.Outlined.Search)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .height(58.dp)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(32.dp), spotColor = ElectricViolet.copy(alpha = 0.25f))
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF271F2E),
                            Color(0xFF1B1522)
                        )
                    )
                )
                .border(1.dp, CardBorder.copy(alpha = 0.6f), RoundedCornerShape(32.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
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
                    targetValue = if (isSelected) ElectricViolet.copy(alpha = 0.16f) else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "tabPillBackground"
                )

                Box(
                    modifier = Modifier
                        .height(46.dp)
                        .clip(RoundedCornerShape(24.dp))
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
    }
}
