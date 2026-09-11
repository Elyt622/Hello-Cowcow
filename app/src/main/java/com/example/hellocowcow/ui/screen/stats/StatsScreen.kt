package com.example.hellocowcow.ui.screen.stats

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.hellocowcow.ui.screen.stats.tabs.CollectionScreen
import com.example.hellocowcow.ui.screen.stats.tabs.TokenScreen
import com.example.hellocowcow.ui.viewmodels.screen.stats.CollectionViewModel
import com.example.hellocowcow.ui.viewmodels.screen.stats.TokenViewModel

@Composable
fun StatsScreen() {
  var tabIndex by remember { mutableIntStateOf(0) }
  val tabs = listOf("CowCow", "MOOVE")

  Column(
    modifier = Modifier.fillMaxSize()
  ) {
    Column(
      modifier = Modifier.padding(
        start = 20.dp,
        top = 20.dp,
        end = 20.dp,
        bottom = 16.dp
      )
    ) {
      Text(
        text = "Collection intelligence",
        style = MaterialTheme.typography.headlineLarge
      )
      Text(
        text = "Market, community and token signals in one place.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    SecondaryTabRow(
      selectedTabIndex = tabIndex,
      containerColor = MaterialTheme.colorScheme.background,
      contentColor = MaterialTheme.colorScheme.primary
    ) {
      tabs.forEachIndexed { index, label ->
        Tab(
          selected = tabIndex == index,
          onClick = { tabIndex = index },
          text = {
            Text(
              text = label,
              style = MaterialTheme.typography.labelLarge
            )
          }
        )
      }
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .weight(1f)
    ) {
      when (tabIndex) {
        0 -> CollectionScreen(
          collectionViewModel = hiltViewModel<CollectionViewModel>()
        )

        else -> TokenScreen(
          viewModel = hiltViewModel<TokenViewModel>()
        )
      }
    }
  }
}
