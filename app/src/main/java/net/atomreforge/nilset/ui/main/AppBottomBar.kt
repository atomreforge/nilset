package net.atomreforge.nilset.ui.main

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import net.atomreforge.nilset.R
import net.atomreforge.nilset.const.AppRoutes


@Composable
fun AppBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        NavigationBarItem(
            selected = currentRoute == AppRoutes.Tab.HOME,
            onClick = { onNavigate(AppRoutes.Tab.HOME) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_home),
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.drawer_home)) },
        )
        NavigationBarItem(
            selected = currentRoute == AppRoutes.Tab.SETTINGS,
            onClick = { onNavigate(AppRoutes.Tab.SETTINGS) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.settings_title)) },
        )
    }
}
