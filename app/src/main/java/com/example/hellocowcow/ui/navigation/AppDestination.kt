package com.example.hellocowcow.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination(
  val route: String,
  val label: String,
  val icon: ImageVector
) {
  data object Explore : AppDestination(
    route = "explore",
    label = "Explore",
    icon = Icons.Filled.Home
  )

  data object Stats : AppDestination(
    route = "stats",
    label = "Stats",
    icon = Icons.Filled.QueryStats
  )

  data object Portfolio : AppDestination(
    route = "portfolio",
    label = "Portfolio",
    icon = Icons.Filled.Person
  )

  companion object {
    val bottomBarItems = listOf(Explore, Stats, Portfolio)
  }
}
