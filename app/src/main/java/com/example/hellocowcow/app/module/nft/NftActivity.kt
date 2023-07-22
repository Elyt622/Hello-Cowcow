package com.example.hellocowcow.app.module.nft

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.hellocowcow.data.response.xoxnoApi.AttributesResponse
import com.example.hellocowcow.data.response.xoxnoApi.NftResponse
import com.example.hellocowcow.data.response.xoxnoApi.OffersInfoResponse
import com.example.hellocowcow.ui.theme.XportalConnectTheme
import com.example.hellocowcow.ui.viewmodels.screen.NftViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import timber.log.Timber.Forest.plant

@AndroidEntryPoint
class NftActivity : ComponentActivity() {

    val viewModel by viewModels<NftViewModel>()

    lateinit var identifier: String

    override fun onCreate(savedInstanceState: Bundle?) {
        plant(Timber.DebugTree())

        super.onCreate(savedInstanceState)
        identifier = intent.getStringExtra("IDENTIFIER").toString()
        viewModel.getNft(identifier)
        setContent {
            XportalConnectTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme
                        .colorScheme
                        .background
                ) { Body() }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MyScaffold(
        title: String,
        upAvailable: Boolean,
        onUpClicked: () -> Unit,
        content: @Composable (PaddingValues) -> Unit
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = TopAppBarDefaults
                        .topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                    navigationIcon = {
                        if (upAvailable) {
                            IconButton(onClick = { onUpClicked() }) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
            },
            containerColor = Color.Transparent,
            content = content
        )
    }

    @Composable
    fun Body() {
        val activity = (LocalContext.current as? Activity)
        val uiState by viewModel.uiState.collectAsState()

        when (uiState) {
            is NftViewModel.UiState.Loading -> {
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

            is NftViewModel.UiState.Error -> {
                Toast.makeText(
                    this,
                    (uiState as NftViewModel.UiState.Error).error,
                    Toast.LENGTH_SHORT
                ).show()
            }

            is NftViewModel.UiState.Success -> {
                (uiState as NftViewModel.UiState.Success).nft.let { nft ->
                    MyScaffold(
                        title = nft.identifier.toString(),
                        upAvailable = true,
                        onUpClicked = { activity?.finish() },
                        content = { OnSuccess(nft = nft) }
                    )

                }
            }
        }
    }
}
@OptIn(
    ExperimentalGlideComposeApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun OnSuccess(nft: NftResponse) {
    Column(
        Modifier
            .padding(
                top = 65.dp,
                start = 8.dp,
                end = 8.dp,
                bottom = 8.dp
            )
    ) {
        ElevatedCard(
            elevation = CardDefaults
                .elevatedCardElevation(16.dp)
        ) {
            if (nft.hasSecondNFT == true)
                HorizontalPager(
                    modifier = Modifier
                        .size(350.dp),
                    pageCount = 2
                ) { index ->
                    GlideImage(
                        model = arrayOf(
                            nft.url,
                            "https://xoxno.com/api/getCow?identifier=${nft.identifier}"
                        )[index],
                        contentDescription = nft.name,
                        modifier = Modifier.scale(
                            scaleY = 1.07F,
                            scaleX = 1.04F
                        )
                    )
                    Text(
                        text = "Upgraded",
                        Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        textAlign = TextAlign.Right,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.background
                    )
                }
            else {
                GlideImage(
                    model = nft.url,
                    contentDescription = nft.name,
                    modifier = Modifier
                        .scale(
                            scaleY = 1.07F,
                            scaleX = 1.04F
                        )
                )
            }
        }
        Row(
            Modifier.fillMaxWidth()
        ) {
            Text(
                text = nft.name.toString(),
                modifier = Modifier
                    .padding(top = 8.dp, start = 8.dp),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Rank: " + nft.metadata?.rarity?.rank.toString(),
                textAlign = TextAlign.End,
                modifier = Modifier
                    .padding(top = 8.dp, end = 8.dp)
                    .fillMaxWidth(),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (nft.onSale == true)
            Text(
                color = MaterialTheme.colorScheme.primary,
                text = nft.saleInfoNft?.maxBidShort.toString()
                        + " "
                        + nft.saleInfoNft?.acceptedPaymentToken,
                modifier = Modifier.
                        padding(
                            start = 8.dp,
                            bottom = 4.dp,
                            top = 4.dp
                        )
            )
        TabScreen(nft)
    }
}

@Composable
fun TabScreen(nft: NftResponse) {

    var tabIndex by remember { mutableStateOf(0) }

    val tabs = listOf("Attributes", "Offers", "Activity")

    Column(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()) {
        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.background
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    text = {
                        Text(
                            title, style = MaterialTheme.typography.labelMedium
                        )
                    },
                    selected = tabIndex == index,
                    onClick = { tabIndex = index },
                    selectedContentColor = MaterialTheme.colorScheme.background,
                    unselectedContentColor = MaterialTheme.colorScheme.background
                )
            }
        }
        when (tabIndex) {
            0 -> AttributesTab(nft.metadata?.attributes!!)
            1 -> OffersTab(nft.hasOffers.toBoolean(), nft.offersInfo)
            2 -> ActivityTab()
        }
    }
}

@Composable
fun AttributesTab(
    attributes: ArrayList<AttributesResponse>
) {
    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.Black
    )
    Column() {
        Row(
            Modifier
                .padding(
                    top = 8.dp,
                    bottom = 8.dp
                )
        ) {
            Text(
                text = "Attributes",
                textAlign = TextAlign.Left,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 8.dp)
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
                text = "Rarity",
                textAlign = TextAlign.Right,
                color = MaterialTheme.colorScheme.primary
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(400.dp),
            content = {
                items(attributes.size) { index ->
                    attributes[index].let { attribute ->
                        ElevatedCard(
                            Modifier.padding(bottom = 4.dp),
                            colors = cardColors,
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(
                                Modifier
                                    .padding(8.dp)
                            ) {
                                Column() {
                                    Text(
                                        text = attribute.traitType.toString() +
                                                ": " + attribute.value,
                                        color = MaterialTheme.colorScheme.background,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(
                                        text = "Floor: " +
                                                String.format(
                                                    "%.2f",
                                                    attribute.FloorPrice
                                                ),
                                        color = MaterialTheme.colorScheme.background,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                Row(
                                    Modifier
                                        .align(Alignment.CenterVertically)
                                        .fillMaxSize(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = attribute.occurance.toString()
                                                + " ("
                                                + String.format("%.2f", attribute.frequency!! *100.0)
                                                + "%)",
                                        color = MaterialTheme.colorScheme.background,
                                        style = MaterialTheme.typography.labelMedium,
                                        textAlign = TextAlign.Right
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun OffersTab(
    nftHasOffers: Boolean,
    offersInfo: ArrayList<OffersInfoResponse>
) {

    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.Black
    )

    if (!nftHasOffers)
        Column(
            Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No offers on this NFT!"
            )
        }
    else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(400.dp),
            modifier = Modifier.padding(top = 4.dp),
            content = {
                items(offersInfo.size) { index ->
                    offersInfo[index].let { offer ->
                        ElevatedCard(
                            Modifier.padding(bottom = 4.dp),
                            colors = cardColors,
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(
                                Modifier
                                    .padding(8.dp)
                            ) {
                                Column() {
                                    Row() {
                                        Text(
                                            text = offer.EgldValue.toString() + " "
                                            + offer.paymentToken,
                                            color = MaterialTheme.colorScheme.background,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                    Row() {
                                        if (offer.ownerUsername != null)
                                            Text(
                                                color = MaterialTheme.colorScheme.background,
                                                style = MaterialTheme.typography.labelMedium,
                                                text = "From: " + offer.ownerUsername.toString()
                                            )
                                        else {
                                            Text(
                                                color = MaterialTheme.colorScheme.background,
                                                style = MaterialTheme.typography.labelMedium,
                                                text = "From: "
                                                        + offer.owner?.substring(0, 6)
                                                        + "..."
                                                        + offer.owner?.substring(
                                                    offer.owner!!.length-5,
                                                    offer.owner!!.length
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}


@Composable
fun ActivityTab() {
    Column(
        Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Soon"
        )
    }
}