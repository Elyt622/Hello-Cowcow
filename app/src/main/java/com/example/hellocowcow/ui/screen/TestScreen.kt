package com.example.hellocowcow.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.hellocowcow.domain.models.DomainAccount
import com.example.hellocowcow.ui.composables.CustomDialog
import com.example.hellocowcow.ui.viewmodels.screen.TestViewModel
import com.walletconnect.sign.client.SignClient
import timber.log.Timber

@Composable
fun TestScreen(
    account: DomainAccount,
    topic: String,
    viewModel: TestViewModel
) {

    val uiState by viewModel.uiState.collectAsState()

    Box(
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = {
                SignClient.request(
                    request = viewModel.buildClaimRewardRequest(account, topic),
                    onError = { err ->
                        Timber.tag("ERROR").e(err.throwable)
                    }
                )
            }
        ) {
            Text(
                text = "Claim Rewards",
                color = Color.Black,
                style = MaterialTheme.typography.labelMedium
            )
        }

        when (uiState) {
            is TestViewModel.UiState.Send -> {
                (uiState as TestViewModel.UiState.Send)
                    .tx.let { tx ->
                        CustomDialog(
                            "Claim Rewards",
                            tx = tx,
                            setShowDialog = { }
                        )
                    }
            }

            is TestViewModel.UiState.Error -> {
                (uiState as TestViewModel.UiState.Error)
                    .error.let { err ->
                        Toast.makeText(
                            LocalContext.current,
                            err,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }

            else -> {}
        }
    }
}