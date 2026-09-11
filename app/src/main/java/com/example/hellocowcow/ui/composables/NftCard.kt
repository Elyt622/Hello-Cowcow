package com.example.hellocowcow.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.hellocowcow.domain.models.DomainNft
import com.example.hellocowcow.ui.theme.Typography2

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NftCard(
  nft: DomainNft,
  onClicked: () -> Unit
) {
  val cardColors = CardDefaults.cardColors(
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = Color.Black
  )

  ElevatedCard(
    colors = cardColors,
    modifier = Modifier.padding(8.dp),
    elevation = CardDefaults.cardElevation(8.dp),
    onClick = onClicked
  ) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      CircularProgressIndicator(
        modifier = Modifier.size(16.dp),
        color = MaterialTheme.colorScheme.background
      )
      GlideImage(
        model = nft.url,
        contentDescription = nft.collection,
        modifier = Modifier.padding(
          start = 8.dp,
          end = 8.dp,
          top = 8.dp
        )
      )
      if (nft.hasSecondNFT == true) {
        Icon(
          modifier = Modifier
            .padding(8.dp)
            .align(Alignment.TopEnd),
          imageVector = Icons.Filled.Upgrade,
          contentDescription = Icons.Filled.Upgrade.name,
          tint = MaterialTheme.colorScheme.background
        )
      }
    }

    Column(
      modifier = Modifier
        .align(Alignment.CenterHorizontally)
        .padding(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        color = MaterialTheme.colorScheme.background,
        text = nft.name.toString(),
        style = Typography2.bodyLarge
      )
    }
  }
}
