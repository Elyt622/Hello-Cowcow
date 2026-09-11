package com.example.hellocowcow.ui.screen.portfolio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConnectWalletScreen(
  connecting: Boolean,
  errorMessage: String? = null,
  primaryLabel: String = "Connect xPortal",
  onPrimaryAction: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text(
      text = "Your CowCow portfolio",
      style = MaterialTheme.typography.headlineSmall
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Explore the collection without a wallet. Connect xPortal only when you want to see your CowCows, staking and MOOVE rewards.",
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (errorMessage != null) {
      Spacer(modifier = Modifier.height(20.dp))
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.errorContainer
        )
      ) {
        Text(
          text = errorMessage,
          modifier = Modifier.padding(16.dp),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onErrorContainer
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
      onClick = onPrimaryAction,
      enabled = !connecting,
      modifier = Modifier.fillMaxWidth()
    ) {
      if (connecting) {
        CircularProgressIndicator(
          modifier = Modifier.height(20.dp),
          strokeWidth = 2.dp,
          color = MaterialTheme.colorScheme.onPrimary
        )
        Text(
          text = "Waiting for xPortal…",
          modifier = Modifier.padding(start = 12.dp)
        )
      } else {
        Text(primaryLabel)
      }
    }
  }
}
