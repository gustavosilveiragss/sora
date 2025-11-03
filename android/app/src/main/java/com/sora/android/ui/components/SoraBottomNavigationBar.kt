package com.sora.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sora.android.R
import com.sora.android.ui.navigation.SoraScreens
import com.sora.android.ui.theme.SoraIcons

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
)

@Composable
fun SoraBottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    unreadNotificationsCount: Int = 0,
    profilePictureUrl: String? = null
) {
    android.util.Log.d("SORA_BOTTOM_NAV", "Renderizando com contador: $unreadNotificationsCount")
    val items = remember {
        listOf(
            BottomNavItem(
                route = SoraScreens.Feed.route,
                icon = SoraIcons.Home
            ),
            BottomNavItem(
                route = SoraScreens.Explore.route,
                icon = SoraIcons.Compass
            ),
            BottomNavItem(
                route = SoraScreens.CreatePost.route,
                icon = SoraIcons.Plus
            ),
            BottomNavItem(
                route = SoraScreens.Notifications.route,
                icon = SoraIcons.Bell
            ),
            BottomNavItem(
                route = SoraScreens.Profile.route,
                icon = SoraIcons.User
            )
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = currentRoute == item.route
                val isCenterButton = index == 2

                if (isCenterButton) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        FloatingActionButton(
                            onClick = { onNavigate(item.route) },
                            modifier = Modifier
                                .size(56.dp)
                                .offset(y = (-12).dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = "",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = { onNavigate(item.route) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box {
                                if (index == 4 && profilePictureUrl != null) {
                                    AsyncImage(
                                        model = profilePictureUrl,
                                        contentDescription = "",
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = CircleShape
                                            ),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = "",
                                        modifier = Modifier.size(24.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (index == 3 && unreadNotificationsCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .align(Alignment.TopEnd)
                                            .offset(x = 4.dp, y = (-4).dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(5.dp))
                        }
                    }
                }
            }
        }
    }
}