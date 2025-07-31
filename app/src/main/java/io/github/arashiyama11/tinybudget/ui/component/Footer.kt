package io.github.arashiyama11.tinybudget.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.screen.Screen
import io.github.arashiyama11.tinybudget.ui.main.MainScreen
import io.github.arashiyama11.tinybudget.ui.main.SettingsScreen

sealed class BottomNavItem(
    val screen: Screen,
    val resourceId: String,
    val icon: @Composable () -> Unit
) {
    object Main :
        BottomNavItem(MainScreen, "Main", { Icon(Icons.Filled.Home, contentDescription = "Main") })

    object Settings : BottomNavItem(
        SettingsScreen,
        "Settings",
        { Icon(Icons.Filled.Settings, contentDescription = "Settings") })
}

@Composable
fun Footer(
    modifier: Modifier = Modifier,
    currentScreen: Screen,
    onTabSelected: (Screen) -> Unit,
) {
    val items = listOf(
        BottomNavItem.Main,
        BottomNavItem.Settings
    )
    NavigationBar(modifier = modifier) {
        items.forEach { item ->
            NavigationBarItem(
                icon = item.icon,
                label = { Text(item.resourceId) },
                selected = currentScreen::class == item.screen::class,
                onClick = { onTabSelected(item.screen) }
            )
        }
    }
}