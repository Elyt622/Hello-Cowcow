package com.example.hellocowcow.ui.composables

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.hellocowcow.data.Data
import com.example.hellocowcow.domain.models.DomainAccount
import com.example.hellocowcow.domain.models.ItemNav
import com.example.hellocowcow.ui.screen.RaffleScreen
import com.example.hellocowcow.ui.screen.home.HomeScreen
import com.example.hellocowcow.ui.screen.profile.ProfileScreen
import com.example.hellocowcow.ui.screen.stats.StatsScreen

@Composable
fun HostController(
  navHostController: NavHostController,
  account: DomainAccount,
  topic: String
) {
  NavHost(
    navController = navHostController,
    startDestination = ItemNav.Home.route
  ) {
    Data.items.forEach { item ->
      composable(item.route) {
        when (it.destination.route) {
          ItemNav.Home.route -> {
            HomeScreen(
              viewModel = hiltViewModel(),
              onCollectionClick = { navHostController.navigate(ItemNav.Stats.route) },
              onPortfolioClick = { navHostController.navigate(ItemNav.Profile.route) }
            )
          }

          ItemNav.Raffles.route -> {
            RaffleScreen()
          }

          ItemNav.Stats.route -> {
            StatsScreen()
          }

          ItemNav.Profile.route -> {
            ProfileScreen(account, topic, hiltViewModel())
          }
        }
      }
    }
  }
}
