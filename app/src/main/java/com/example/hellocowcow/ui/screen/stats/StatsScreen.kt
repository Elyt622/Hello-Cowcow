package com.example.hellocowcow.ui.screen.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.hellocowcow.ui.screen.stats.tabs.CollectionScreen
import com.example.hellocowcow.ui.screen.stats.tabs.TokenScreen
import com.example.hellocowcow.ui.viewmodels.screen.stats.CollectionViewModel
import com.example.hellocowcow.ui.viewmodels.screen.stats.TokenViewModel

@Composable
fun StatsScreen() {
  Surface(
    Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "Moove Forward.",
        fontSize = 22.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
          .padding(top = 20.dp),
        fontWeight = FontWeight.Bold
      )

      Text(
        text = "Moove Together.",
        fontSize = 22.sp,
        color = MaterialTheme.colorScheme.onPrimary,
        fontWeight = FontWeight.ExtraBold
      )
    }
    TabScreen()
  }
}

@Composable
fun TabScreen() {
  var tabIndex by remember { mutableIntStateOf(0) }
  val tabs = listOf("Moove", "Cow")

  Column(
    modifier = Modifier
      .padding(top = 90.dp)
      .fillMaxWidth()
  ) {
    TabRow(
      selectedTabIndex = tabIndex,
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.background
    ) {
      tabs.forEachIndexed { index, title ->
        Tab(
          text = { Text(title, style = MaterialTheme.typography.bodyLarge) },
          selected = tabIndex == index,
          onClick = { tabIndex = index },
          selectedContentColor = MaterialTheme.colorScheme.background,
          unselectedContentColor = MaterialTheme.colorScheme.background
        )
      }
    }
    when (tabIndex) {
      0 -> {
        val viewModel: TokenViewModel = hiltViewModel()
        TokenScreen(viewModel)
      }

      1 -> {
        val viewModel: CollectionViewModel = hiltViewModel()
        CollectionScreen(viewModel)
      }
    }
  }
}
