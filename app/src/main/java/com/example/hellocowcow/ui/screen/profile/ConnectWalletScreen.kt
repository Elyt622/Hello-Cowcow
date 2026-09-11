package com.example.hellocowcow.ui.screen.profile

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ConnectWalletScreen(
  isConnecting: Boolean,
  error: String?,
  onConnect: () -> Unit,
  onContinueAsGuest: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
      )
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Your CowCow portfolio",
          style = MaterialTheme.typography.headlineSmall,
          textAlign = TextAlign.Center
        )
        Text(
          text = "Connect xPortal to see your CowCows, staking and MOOVE rewards. Explore and collection stats stay available without a wallet.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )

        if (error != null) {
          Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
          onClick = onConnect,
          enabled = !isConnecting,
          modifier = Modifier.fillMaxWidth()
        ) {
          if (isConnecting) {
            CircularProgressIndicator(
              modifier = Modifier.height(20.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary
            )
          } else {
            Text("Connect xPortal")
          }
        }

        if (isConnecting) {
          Text(
            text = "Confirm the connection in xPortal, then return to Hello CowCow.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )
        }

        TextButton(onClick = onContinueAsGuest) {
          Text("Continue browsing")
        }
      }
    }
  }
}
