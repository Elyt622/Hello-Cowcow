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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
            text = "CowCow detail",
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
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.errorContainer
      )
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = "Unable to load this CowCow",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onErrorContainer
        )
        Text(
          text = message,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onErrorContainer
        )
      }
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
  val locale = LocalLocale.current.platformLocale

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(padding)
      .verticalScroll(rememberScrollState())
      .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp)
  ) {
    NftArtwork(nft)
    NftIdentity(nft)
    NftQuickFacts(nft, locale)

    nft.metadata?.description
      ?.takeIf { it.isNotBlank() }
      ?.let { description ->
        Text(
          text = description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

    if (nft.onSale == true) {
      ListingCard(nft)
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
  val image = nft.avifUrl ?: nft.webpUrl ?: nft.url

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .aspectRatio(1f),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
  ) {
    if (nft.hasSecondNFT == true) {
      val pagerState = rememberPagerState(pageCount = { 2 })
      HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
      ) { index ->
        Box(modifier = Modifier.fillMaxSize()) {
          GlideImage(
            model = if (index == 0) {
              image
            } else {
              "https://xoxno.com/api/getCow?identifier=${nft.identifier}"
            },
            contentDescription = nft.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
          )

          StatusPill(
            text = if (index == 0) "Original" else "Upgraded",
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(14.dp)
          )
        }
      }
    } else {
      GlideImage(
        model = image,
        contentDescription = nft.name,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
      )
    }
  }
}

@Composable
private fun NftIdentity(nft: DomainNft) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(
        text = nft.name ?: "CowCow",
        style = MaterialTheme.typography.headlineMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = nft.identifier.orEmpty(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    Surface(
      shape = RoundedCornerShape(18.dp),
      color = MaterialTheme.colorScheme.primaryContainer,
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "RANK",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f)
        )
        Text(
          text = "#${nft.metadata?.rarity?.rank ?: "—"}",
          style = MaterialTheme.typography.titleMedium
        )
      }
    }
  }
}

@Composable
private fun NftQuickFacts(
  nft: DomainNft,
  locale: Locale
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    QuickFact(
      label = "Floor",
      value = nft.floorValue?.let {
        "${String.format(locale, "%.2f", it)} EGLD"
      } ?: "—",
      modifier = Modifier.weight(1f)
    )
    QuickFact(
      label = "Status",
      value = if (nft.onSale == true) "Listed" else "Held",
      modifier = Modifier.weight(1f)
    )
    QuickFact(
      label = "Upgrade",
      value = if (nft.hasSecondNFT == true) "Yes" else "No",
      modifier = Modifier.weight(1f)
    )
  }
}

@Composable
private fun QuickFact(
  label: String,
  value: String,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceVariant
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun ListingCard(nft: DomainNft) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(
        text = "Currently listed",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
      )
      Text(
        text = "${nft.saleInfoNft?.maxBidShort ?: "—"} ${nft.saleInfoNft?.acceptedPaymentToken.orEmpty()}",
        style = MaterialTheme.typography.titleLarge
      )
    }
  }
}

@Composable
private fun StatusPill(
  text: String,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(100.dp),
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
    contentColor = MaterialTheme.colorScheme.onSurface
  ) {
    Text(
      text = text,
      modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
      style = MaterialTheme.typography.labelSmall
    )
  }
}

@Composable
private fun NftTabs(nft: DomainNft) {
  var tabIndex by remember { mutableIntStateOf(0) }
  val tabs = listOf("Attributes", "Offers")

  Column(
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    SecondaryTabRow(
      selectedTabIndex = tabIndex,
      containerColor = MaterialTheme.colorScheme.background,
      contentColor = MaterialTheme.colorScheme.primary
    ) {
      tabs.forEachIndexed { index, title ->
        Tab(
          selected = tabIndex == index,
          onClick = { tabIndex = index },
          text = {
            Text(
              text = title,
              style = MaterialTheme.typography.labelLarge
            )
          }
        )
      }
    }

    when (tabIndex) {
      0 -> AttributesTab(nft.metadata?.attributes.orEmpty())
      else -> OffersTab(
        nftHasOffers = nft.hasOffers?.toBoolean() == true,
        offersInfo = nft.offersInfo
      )
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
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    attributes.forEach { attribute ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Text(
              text = attribute.traitType ?: "Trait",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = attribute.value ?: "—",
              style = MaterialTheme.typography.titleSmall
            )
            Text(
              text = attribute.floorPrice?.let {
                "Trait floor ${String.format(locale, "%.2f", it)} EGLD"
              } ?: "No trait floor",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Column(
            horizontalAlignment = Alignment.End
          ) {
            Text(
              text = "${String.format(locale, "%.1f", (attribute.frequency ?: 0.0) * 100.0)}%",
              style = MaterialTheme.typography.titleSmall
            )
            Text(
              text = "${attribute.occurance ?: 0} items",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.End
            )
          }
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
    EmptySection("No active offers on this CowCow")
    return
  }

  Column(
    verticalArrangement = Arrangement.spacedBy(10.dp)
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(
            verticalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Text(
              text = "Offer",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "${offer.EgldValue ?: "—"} ${offer.paymentToken.orEmpty()}",
              style = MaterialTheme.typography.titleSmall
            )
          }
          Text(
            text = owner,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
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
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}
