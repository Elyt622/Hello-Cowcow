package com.example.hellocowcow.ui.composables

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.hellocowcow.ui.navigation.AppDestination

@Composable
fun BottomBar(navController: NavController) {
  val navStackBackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navStackBackEntry?.destination?.route

  NavigationBar(
    containerColor = MaterialTheme.colorScheme.background
  ) {
    AppDestination.bottomBarItems.forEach { destination ->
      val selected = currentRoute == destination.route

      NavigationBarItem(
        icon = {
          Icon(
            imageVector = destination.icon,
            contentDescription = destination.label
          )
        },
        label = { Text(destination.label) },
        selected = selected,
        onClick = {
          navController.navigate(destination.route) {
            navController.graph.startDestinationRoute?.let { startRoute ->
              popUpTo(startRoute) { saveState = true }
            }
            launchSingleTop = true
            restoreState = true
          }
        },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = MaterialTheme.colorScheme.onPrimary,
          selectedTextColor = MaterialTheme.colorScheme.primary,
          indicatorColor = MaterialTheme.colorScheme.primary,
          unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
          unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
      )
    }
  }
}
