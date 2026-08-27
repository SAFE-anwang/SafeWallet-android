package io.horizontalsystems.bankwallet.modules.nftv2.collection

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.nftv2.asset.NftAssetFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize

class NftCollectionFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            NftCollectionScreen(navController, input)
        }
    }

    @Parcelize
    data class Input(
        val blockchainType: BlockchainType,
        val contractAddress: String,
        val collectionName: String,
    ) : Parcelable
}

@Composable
private fun NftCollectionScreen(
    navController: NavController,
    input: NftCollectionFragment.Input
) {
    val viewModel = viewModel<NftCollectionViewModel>(
        factory = NftCollectionViewModel.Factory(
            input.blockchainType,
            input.contractAddress,
            input.collectionName
        )
    )
    val uiState = viewModel.uiState
    val view = LocalView.current

    HSScaffold(
        title = stringResource(R.string.Nft_Collection_Title),
        onBack = { navController.popBackStack() }
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 集合信息头
            item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ComposeAppTheme.colors.raina),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.iconUrl != null) {
                        AsyncImage(
                            model = uiState.iconUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = uiState.collectionName.take(1).uppercase(),
                            style = ComposeAppTheme.typography.headline2,
                            color = ComposeAppTheme.colors.grey
                        )
                    }
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    body_leah(
                        text = uiState.collectionName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    VSpacer(4.dp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = uiState.standard,
                            style = ComposeAppTheme.typography.microSB,
                            color = ComposeAppTheme.colors.grey,
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = ComposeAppTheme.colors.steel20,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                        Text(
                            text = uiState.contractAddress.shorten(),
                            style = ComposeAppTheme.typography.caption,
                            color = ComposeAppTheme.colors.grey,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_copy_20),
                            contentDescription = stringResource(R.string.Hud_Text_Copied),
                            tint = ComposeAppTheme.colors.grey,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(16.dp)
                                .clickable {
                                    TextHelper.copyText(uiState.contractAddress)
                                    HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                                }
                        )
                    }
                }
            }
            }

            // 合集描述
            uiState.description?.let { description ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    subhead2_grey(
                        text = description,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            // 24h 统计
            item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                NftStatCell(
                    uiState = uiState,
                    value = uiState.floorPrice24h,
                    label = stringResource(R.string.Nft_FloorPrice24h),
                    modifier = Modifier.weight(1f)
                )
                NftStatCell(
                    uiState = uiState,
                    value = uiState.averagePrice24h,
                    label = stringResource(R.string.Nft_AveragePrice24h),
                    modifier = Modifier.weight(1f)
                )
                NftStatCell(
                    uiState = uiState,
                    value = uiState.volume24h,
                    label = stringResource(R.string.Nft_Volume24h),
                    modifier = Modifier.weight(1f)
                )
            }
            }

            // 资产标题
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    body_leah(
                        text = stringResource(R.string.Nft_Assets),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_blocks_20),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.grey,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // NFT 网格
            items(
                items = uiState.assets,
                key = { it.tokenId }
            ) { asset ->
                NftAssetCard(asset) {
                    navController.slideFromRight(
                        R.id.nftAssetFragment,
                        NftAssetFragment.Input(
                            input.blockchainType,
                            input.contractAddress,
                            asset.tokenId,
                            uiState.collectionName
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun NftStatCell(
    uiState: NftCollectionUiState,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoinImage(
                token = uiState.baseToken,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = value,
                style = ComposeAppTheme.typography.subhead,
                color = ComposeAppTheme.colors.leah,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        VSpacer(4.dp)
        caption_grey(text = label)
    }
}

@Composable
private fun NftAssetCard(
    asset: NftAssetViewItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(ComposeAppTheme.colors.raina),
            contentAlignment = Alignment.Center
        ) {
            if (asset.imageUrl != null) {
                AsyncImage(
                    model = asset.imageUrl,
                    contentDescription = asset.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = "?",
                    style = ComposeAppTheme.typography.title1,
                    color = ComposeAppTheme.colors.steel20
                )
            }
        }
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = asset.name,
                style = ComposeAppTheme.typography.subhead,
                color = ComposeAppTheme.colors.leah,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            VSpacer(2.dp)
            Text(
                text = if (asset.balance > 1) "#${asset.tokenId}  x${asset.balance}" else "#${asset.tokenId}",
                style = ComposeAppTheme.typography.caption,
                color = ComposeAppTheme.colors.grey,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
