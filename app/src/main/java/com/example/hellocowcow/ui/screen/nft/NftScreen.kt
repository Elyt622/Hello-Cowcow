package com.example.hellocowcow.ui.screen.nft

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.hellocowcow.data.retrofit.proxyXoxnoApi.Attributes
import com.example.hellocowcow.data.retrofit.proxyXoxnoApi.OffersInfo
import com.example.hellocowcow.domain.models.DomainNft
import com.example.hellocowcow.ui.viewmodels.screen.nft.NftViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NftScreen(
  identifier: String,
  viewModel: NftViewModel,
  onBack: () -> Unit
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(identifier) {
    viewModel.load(identifier)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = (uiState as? NftViewModel.UiState.Success)
              ?.nft
              ?.identifier
              ?: identifier,
            style = MaterialTheme.typography.titleMedium
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back"
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background
        )
      )
    }
  ) { padding ->
    when (val state = uiState) {
      NftViewModel.UiState.Loading -> LoadingNft(padding)
      is NftViewModel.UiState.Error -> ErrorNft(
        padding = padding,
        message = state.message
      )
      is NftViewModel.UiState.Success -> NftContent(
        padding = padding,
        nft = state.nft
      )
    }
  }
}

@Composable
private fun LoadingNft(padding: PaddingValues) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(padding),
    contentAlignment = Alignment.Center
  ) {
    CircularProgressIndicator()
  }
}

@Composable
private fun ErrorNft(
  padding: PaddingValues,
  message: String
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(padding)
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.errorContainer
      )
    ) {
      Text(
        text = message,
        modifier = Modifier.padding(20.dp),
        color = MaterialTheme.colorScheme.onErrorContainer
      )
    }
  }
}

@OptIn(
  ExperimentalGlideComposeApi::class,
  ExperimentalFoundationApi::class
)
@Composable
private fun NftContent(
  padding: PaddingValues,
  nft: DomainNft
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(padding)
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    NftArtwork(nft)

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Text(
          text = nft.name.orEmpty(),
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.primary
        )
        Text(
          text = nft.identifier.orEmpty(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Text(
        text = "Rank #${nft.metadata?.rarity?.rank ?: "—"}",
        style = MaterialTheme.typography.titleMedium
      )
    }

    if (nft.onSale == true) {
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        )
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = "Listed",
            style = MaterialTheme.typography.labelLarge
          )
          Text(
            text = "${nft.saleInfoNft?.maxBidShort ?: "—"} ${nft.saleInfoNft?.acceptedPaymentToken.orEmpty()}",
            style = MaterialTheme.typography.titleMedium
          )
        }
      }
    }

    NftTabs(nft)
  }
}

@OptIn(
  ExperimentalGlideComposeApi::class,
  ExperimentalFoundationApi::class
)
@Composable
private fun NftArtwork(nft: DomainNft) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .aspectRatio(1f),
    shape = RoundedCornerShape(24.dp)
  ) {
    if (nft.hasSecondNFT == true) {
      val pagerState = rememberPagerState(pageCount = { 2 })
      HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
      ) { index ->
        Box(
          modifier = Modifier.fillMaxSize()
        ) {
          GlideImage(
            model = if (index == 0) {
              nft.url
            } else {
              "https://xoxno.com/api/getCow?identifier=${nft.identifier}"
            },
            contentDescription = nft.name,
            modifier = Modifier.fillMaxSize()
          )
          if (index == 1) {
            Card(
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
            ) {
              Text(
                text = "Upgraded",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
              )
            }
          }
        }
      }
    } else {
      GlideImage(
        model = nft.url,
        contentDescription = nft.name,
        modifier = Modifier.fillMaxSize()
      )
    }
  }
}

@Composable
private fun NftTabs(nft: DomainNft) {
  var tabIndex by remember { mutableIntStateOf(0) }
  val tabs = listOf("Attributes", "Offers", "Activity")

  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    SecondaryTabRow(
      selectedTabIndex = tabIndex,
      containerColor = MaterialTheme.colorScheme.surfaceVariant,
      contentColor = MaterialTheme.colorScheme.primary
    ) {
      tabs.forEachIndexed { index, title ->
        Tab(
          selected = tabIndex == index,
          onClick = { tabIndex = index },
          text = { Text(title) }
        )
      }
    }

    when (tabIndex) {
      0 -> AttributesTab(nft.metadata?.attributes.orEmpty())
      1 -> OffersTab(
        nftHasOffers = nft.hasOffers.toBoolean(),
        offersInfo = nft.offersInfo
      )
      else -> ActivityTab()
    }
  }
}

@Composable
private fun AttributesTab(attributes: List<Attributes>) {
  val locale = LocalLocale.current.platformLocale

  if (attributes.isEmpty()) {
    EmptySection("No attributes available")
    return
  }

  Column(
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    attributes.forEach { attribute ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(
            modifier = Modifier.weight(1f)
          ) {
            Text(
              text = "${attribute.traitType}: ${attribute.value}",
              style = MaterialTheme.typography.titleSmall
            )
            Text(
              text = attribute.floorPrice?.let {
                "Floor: ${formatNumber(locale, "%.2f", it)} EGLD"
              } ?: "Floor: None",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Text(
            text = "${attribute.occurance ?: 0} (${formatNumber(locale, "%.2f", (attribute.frequency ?: 0.0) * 100.0)}%)",
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.End
          )
        }
      }
    }
  }
}

@Composable
private fun OffersTab(
  nftHasOffers: Boolean,
  offersInfo: List<OffersInfo>
) {
  if (!nftHasOffers || offersInfo.isEmpty()) {
    EmptySection("No offers on this NFT")
    return
  }

  Column(
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    offersInfo.forEach { offer ->
      val owner = offer.ownerUsername
        ?: offer.owner?.let { address ->
          if (address.length > 12) {
            "${address.take(6)}…${address.takeLast(5)}"
          } else {
            address
          }
        }
        ?: "Unknown"

      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "${offer.EgldValue ?: "—"} ${offer.paymentToken.orEmpty()}",
              style = MaterialTheme.typography.titleSmall
            )
            Text(
              text = "From $owner",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ActivityTab() {
  EmptySection("Activity timeline coming next")
}

@Composable
private fun EmptySection(message: String) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 32.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = message,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

private fun formatNumber(
  locale: Locale,
  pattern: String,
  value: Double
): String = String.format(locale, pattern, value)
