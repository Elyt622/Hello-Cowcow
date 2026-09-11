package com.example.hellocowcow.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppDestination : NavKey

@Serializable
data object ExploreDestination : AppDestination

@Serializable
data object CollectionDestination : AppDestination

@Serializable
data object PortfolioDestination : AppDestination

@Serializable
data object RecoveryDestination : AppDestination

@Serializable
data class NftDetailDestination(
  val identifier: String
) : AppDestination
