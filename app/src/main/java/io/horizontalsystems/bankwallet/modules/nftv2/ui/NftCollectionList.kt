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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.order
import io.horizontalsystems.bankwallet.core.supportedNftTypes
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.nftv2.NftCollectionListViewModel
import io.horizontalsystems.bankwallet.modules.nftv2.NftCollectionViewItem
import io.horizontalsystems.bankwallet.modules.nftv2.NftListTab
import io.horizontalsystems.bankwallet.modules.nftv2.collection.NftCollectionFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.marketkit.models.BlockchainType

@Composable
fun NftCollectionList(
    navController: NavController,
    viewModel: NftCollectionListViewModel,
    modifier: Modifier = Modifier
) {
    val uiState = viewModel.uiState

    HSSwipeRefresh(
        refreshing = uiState.syncing,
        modifier = modifier,
        onRefresh = viewModel::refresh
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeAppTheme.colors.tyler),
        ) {
            when (uiState.viewState) {
                ViewState.Success -> {
                    if (uiState.collections.isEmpty()) {
                        item {
                            NftEmptyBlock(
                                text = if (uiState.tab == NftListTab.Favorites)
                                    stringResource(R.string.Nft_Favorites_Empty)
                                else stringResource(R.string.Nft_EmptyList)
                            )
                        }
                    } else {
                        // 按链分组展示：以支持的 NFT 链为骨架，
                        // 即使某条链没有数据也显示分组标题，内容区提示「暂未发现 NFT 合集」
                        val groupedCollections = uiState.collections
                            .groupBy { it.blockchainType }
                            .mapValues { (_, items) -> items.sortedByDescending { it.count } }
                        // 支持的链优先，其余有数据的链按原有顺序追加
                        val blockchains = (
                                supportedNftBlockchains() + groupedCollections.keys
                                ).distinct()

                        blockchains.forEach { blockchainType ->
                            val collections = groupedCollections[blockchainType].orEmpty()
                            item(key = "header-${blockchainType.uid}") {
                                BlockchainHeader(
                                    name = blockchainName(blockchainType),
                                    count = collections.size
                                )
                            }
                            if (collections.isEmpty()) {
                                item(key = "empty-${blockchainType.uid}") {
                                    EmptyChainCell()
                                }
                            } else {
                                items(
                                    items = collections,
                                    key = { "${it.blockchainType.uid}-${it.contractAddress}" }
                                ) { collection ->
                                    NftCollectionCell(collection) {
                                        navController.slideFromRight(
                                            R.id.nftCollectionFragment,
                                            NftCollectionFragment.Input(
                                                // 收藏按账户隔离，详情页需知道当前账户
                                                App.accountManager.activeAccount?.id.orEmpty(),
                                                collection.blockchainType,
                                                collection.contractAddress,
                                                collection.name
                                            )
                                        )
                                    }
                                }
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

/**
 * 支持的 NFT 链（有 [supportedNftTypes] 的链即为支持 NFT 的链）。
 *
 * 用于固定展示分组标题，保证某条链暂无 NFT 时也能看到该分类。
 */
private fun supportedNftBlockchains(): List<BlockchainType> {
    return App.marketKit.allBlockchains()
        .map { it.type }
        .filter { it.supportedNftTypes.isNotEmpty() }
        .distinct()
        .sortedBy { it.order }
}

/** 某条链下暂无 NFT 合集时的占位 */
@Composable
private fun EmptyChainCell() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = stringResource(R.string.Nft_No_Collection_In_Chain),
            style = ComposeAppTheme.typography.subheadB,
            color = ComposeAppTheme.colors.grey
        )
    }
}

/** 链分组标题：链名 + 该链下的合集数量 */
@Composable
private fun BlockchainHeader(name: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ComposeAppTheme.colors.raina)
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = ComposeAppTheme.typography.headline2,
            color = ComposeAppTheme.colors.leah,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stringResource(R.string.Nft_Collection_Count, count),
            style = ComposeAppTheme.typography.caption,
            color = ComposeAppTheme.colors.grey
        )
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
            // 统一使用 NFT 占位图：无图、加载中、加载失败都显示占位图，
            // 避免出现单字母文本或被裁剪的空白
            AsyncImage(
                model = collection.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.icon_24_nft_placeholder),
                error = painterResource(R.drawable.icon_24_nft_placeholder),
                fallback = painterResource(R.drawable.icon_24_nft_placeholder)
            )
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
            // 链名已由分组标题体现，这里显示该合约下的 NFT 数量
            Text(
                text = stringResource(R.string.Nft_Item_Count, collection.count),
                style = ComposeAppTheme.typography.caption,
                color = ComposeAppTheme.colors.grey,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun NftEmptyBlock(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VSpacer(100.dp)
        subhead2_grey(
            text = text,
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
