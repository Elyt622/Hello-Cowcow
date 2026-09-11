package com.example.hellocowcow.ui.screen.portfolio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
      .padding(20.dp),
    verticalArrangement = Arrangement.Center
  ) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(30.dp),
      color = MaterialTheme.colorScheme.primaryContainer,
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
          contentColor = MaterialTheme.colorScheme.primary
        ) {
          Icon(
            imageVector = Icons.Filled.AccountBalanceWallet,
            contentDescription = null,
            modifier = Modifier
              .padding(13.dp)
              .size(30.dp)
          )
        }

        Column(
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "Bring your herd into view",
            style = MaterialTheme.typography.headlineMedium
          )
          Text(
            text = "Connect xPortal to see the CowCows you own, what you have staked or listed, and the MOOVE rewards waiting for you.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
          )
        }

        Column(
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          WalletBenefit("Owned, staked and listed CowCows")
          WalletBenefit("MOOVE rewards and claim status")
          WalletBenefit("Signatures stay inside xPortal")
        }

        if (errorMessage != null) {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.errorContainer
            )
          ) {
            Text(
              text = errorMessage,
              modifier = Modifier.padding(14.dp),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer
            )
          }
        }

        Button(
          onClick = onPrimaryAction,
          enabled = !connecting,
          modifier = Modifier.fillMaxWidth()
        ) {
          if (connecting) {
            CircularProgressIndicator(
              modifier = Modifier.size(19.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
              text = "Waiting for xPortal…",
              modifier = Modifier.padding(start = 10.dp)
            )
          } else {
            Text(primaryLabel)
          }
        }
      }
    }

    Text(
      text = "Explore and Collection remain available without connecting a wallet.",
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
private fun WalletBenefit(text: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Icon(
      imageVector = Icons.Filled.CheckCircle,
      contentDescription = null,
      modifier = Modifier.size(18.dp),
      tint = MaterialTheme.colorScheme.primary
    )
    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onPrimaryContainer
    )
  }
}
