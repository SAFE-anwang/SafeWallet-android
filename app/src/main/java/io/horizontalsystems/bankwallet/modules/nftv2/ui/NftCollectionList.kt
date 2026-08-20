package io.horizontalsystems.bankwallet.modules.nftv2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.nftv2.NftCollectionListViewModel
import io.horizontalsystems.bankwallet.modules.nftv2.NftCollectionViewItem
import io.horizontalsystems.bankwallet.modules.nftv2.collection.NftCollectionFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.marketkit.models.BlockchainType

@Composable
fun NftCollectionList(
    navController: NavController,
    viewModel: NftCollectionListViewModel = viewModel(factory = NftCollectionListViewModel.Factory())
) {
    val uiState = viewModel.uiState

    HSSwipeRefresh(
        refreshing = uiState.syncing,
        onRefresh = viewModel::refresh
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeAppTheme.colors.lawrence),
        ) {
            when (uiState.viewState) {
                ViewState.Success -> {
                    if (uiState.collections.isEmpty()) {
                        item { NftEmptyBlock() }
                    } else {
                        items(
                            items = uiState.collections,
                            key = { "${it.blockchainType.uid}-${it.contractAddress}" }
                        ) { collection ->
                            NftCollectionCell(collection) {
                                navController.slideFromRight(
                                    R.id.nftCollectionFragment,
                                    NftCollectionFragment.Input(
                                        collection.blockchainType,
                                        collection.contractAddress,
                                        collection.name
                                    )
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
            item { VSpacer(70.dp) }
        }
    }
}

@Composable
private fun NftCollectionCell(
    collection: NftCollectionViewItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ComposeAppTheme.colors.tyler)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(ComposeAppTheme.colors.raina),
            contentAlignment = Alignment.Center
        ) {
            if (collection.imageUrl != null) {
                AsyncImage(
                    model = collection.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = collection.name.take(1).uppercase(),
                    style = ComposeAppTheme.typography.headline2,
                    color = ComposeAppTheme.colors.grey
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = collection.name,
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.leah,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            VSpacer(3.dp)
            Text(
                text = blockchainName(collection.blockchainType),
                style = ComposeAppTheme.typography.caption,
                color = ComposeAppTheme.colors.grey,
                maxLines = 1
            )
        }

        Text(
            text = collection.count.toString(),
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah
        )
    }
}

@Composable
private fun NftEmptyBlock() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VSpacer(100.dp)
        subhead2_grey(
            text = stringResource(R.string.Nft_EmptyList),
            textAlign = TextAlign.Center
        )
        VSpacer(32.dp)
    }
}

fun blockchainName(blockchainType: BlockchainType): String = when (blockchainType) {
    BlockchainType.Ethereum -> "Ethereum"
    BlockchainType.BinanceSmartChain -> "BNB Chain"
    BlockchainType.Polygon -> "Polygon"
    else -> blockchainType.uid
}
