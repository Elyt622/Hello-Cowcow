package com.example.hellocowcow.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hellocowcow.domain.models.DomainTransaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAlert(
  tx: DomainTransaction
) {
  val uriHandler = LocalUriHandler.current
  val openDialog = remember { mutableStateOf(true) }
  val status = tx.status.ifBlank { "submitted" }
  val successful = status.equals("success", ignoreCase = true) ||
      status.equals("executed", ignoreCase = true)
  val txHash = tx.txHash.orEmpty()

  if (!openDialog.value) return

  BasicAlertDialog(
    onDismissRequest = { openDialog.value = false }
  ) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (successful) {
                MaterialTheme.colorScheme.secondaryContainer
              } else {
                MaterialTheme.colorScheme.primaryContainer
              },
              contentColor = if (successful) {
                MaterialTheme.colorScheme.onSecondaryContainer
              } else {
                MaterialTheme.colorScheme.onPrimaryContainer
              }
            ) {
              Icon(
                imageVector = if (successful) Icons.Filled.CheckCircle else Icons.Filled.HourglassTop,
                contentDescription = null,
                modifier = Modifier
                  .padding(10.dp)
                  .size(22.dp)
              )
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
              Text(
                text = if (successful) "Transaction confirmed" else "Transaction submitted",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = status.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          IconButton(onClick = { openDialog.value = false }) {
            Icon(
              imageVector = Icons.Filled.Close,
              contentDescription = "Close"
            )
          }
        }

        if (txHash.isNotBlank()) {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Text(
                text = "Transaction hash",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = shortHash(txHash),
                style = MaterialTheme.typography.titleSmall
              )
            }
          }

          Button(
            onClick = {
              uriHandler.openUri(
                "https://explorer.multiversx.com/transactions/$txHash"
              )
              openDialog.value = false
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("View on MultiversX Explorer")
            Icon(
              imageVector = Icons.Filled.OpenInNew,
              contentDescription = null,
              modifier = Modifier
                .padding(start = 8.dp)
                .size(17.dp)
            )
          }
        } else {
          Text(
            text = "The transaction was submitted, but no transaction hash was returned.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

private fun shortHash(hash: String): String = when {
  hash.length <= 20 -> hash
  else -> "${hash.take(9)}…${hash.takeLast(8)}"
}
