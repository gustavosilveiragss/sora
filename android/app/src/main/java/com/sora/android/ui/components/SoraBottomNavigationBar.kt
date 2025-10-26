package com.sora.android.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sora.android.R
import com.sora.android.ui.navigation.SoraScreens
import com.sora.android.ui.theme.SoraIcons

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val labelRes: Int,
    val contentDescription: String
)

@Composable
fun SoraBottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem(
            route = SoraScreens.MainGlobe.route,
            icon = SoraIcons.Globe,
            labelRes = R.string.nav_globe,
            contentDescription = stringResource(R.string.nav_globe),
        ),
        BottomNavItem(
            route = SoraScreens.Explore.route,
            icon = SoraIcons.Compass,
            labelRes = R.string.nav_explore,
            contentDescription = stringResource(R.string.nav_explore),
        ),
        BottomNavItem(
            route = SoraScreens.CreatePost.route,
            icon = SoraIcons.Plus,
            labelRes = R.string.nav_create,
            contentDescription = stringResource(R.string.nav_create),
        ),
        BottomNavItem(
            route = SoraScreens.Profile.route,
            icon = SoraIcons.User,
            labelRes = R.string.nav_profile,
            contentDescription = stringResource(R.string.nav_profile),
        )
    )

    NavigationBar(
        modifier = modifier
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.contentDescription,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = stringResource(item.labelRes),
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                selected = isSelected,
                onClick = { onNavigate(item.route) }
            )
        }
    }
}