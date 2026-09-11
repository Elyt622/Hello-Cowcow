package com.example.hellocowcow.app.module.main.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hellocowcow.app.module.BaseActivity
import com.example.hellocowcow.ui.composables.MainScaffold
import com.example.hellocowcow.ui.theme.HelloCowCowTheme
import com.example.hellocowcow.ui.viewmodels.activity.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import es.dmoral.toasty.Toasty

@AndroidEntryPoint
class MainActivity : BaseActivity() {

  private val viewModel by viewModels<MainViewModel>()

  private lateinit var address: String
  private lateinit var topic: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    address = intent.getStringExtra("ADDRESS").toString()
    topic = intent.getStringExtra("TOPIC").toString()

    viewModel.getAccount(address)

    setContent {
      val uiState by viewModel.currentAccount.collectAsState()
      HelloCowCowTheme(dynamicColor = false) {
        Surface(
          modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
        ) {
          when (uiState) {
            is MainViewModel.UiState.Loading -> {
              Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
              ) {
                CircularProgressIndicator(
                  modifier = Modifier.width(60.dp),
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
            is MainViewModel.UiState.Success -> {
              (uiState as MainViewModel.UiState.Success)
                .data.let { account ->
                  MainScaffold(account, topic)
                }
            }
            is MainViewModel.UiState.Error -> {
              (uiState as MainViewModel.UiState.Error)
                .error.let { err ->
                  Toasty.error(
                    baseContext,
                    err,
                    Toast.LENGTH_SHORT
                  ).show()
                }
            }
          }
        }
      }
    }
  }
}
