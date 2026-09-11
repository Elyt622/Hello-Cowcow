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
