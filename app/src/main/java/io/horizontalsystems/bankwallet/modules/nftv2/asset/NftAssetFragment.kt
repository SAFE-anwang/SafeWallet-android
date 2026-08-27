package io.horizontalsystems.bankwallet.modules.nftv2.asset

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.nftv2.send.SendNftFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize

class NftAssetFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            NftAssetScreen(navController, input)
        }
    }

    @Parcelize
    data class Input(
        val blockchainType: BlockchainType,
        val contractAddress: String,
        val tokenId: String,
        val collectionName: String,
    ) : Parcelable
}

@Composable
private fun NftAssetScreen(
    navController: NavController,
    input: NftAssetFragment.Input
) {
    val viewModel = viewModel<NftAssetViewModel>(
        factory = NftAssetViewModel.Factory(
            input.blockchainType,
            input.contractAddress,
            input.tokenId,
            input.collectionName
        )
    )
    val uiState = viewModel.uiState

    HSScaffold(
        title = stringResource(R.string.Nft_Asset_Title),
        onBack = { navController.popBackStack() },
        bottomBar = {
            if (uiState.recordExists) {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    title = stringResource(R.string.Balance_Send),
                    onClick = {
                        navController.slideFromRight(
                            R.id.sendNftFragment,
                            SendNftFragment.Input(
                                input.blockchainType,
                                input.contractAddress,
                                input.tokenId,
                                uiState.collectionName,
                                uiState.imageUrl,
                                uiState.nftType.name
                            )
                        )
                    }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // NFT 大图
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ComposeAppTheme.colors.raina),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.imageUrl != null -> {
                        AsyncImage(
                            model = uiState.imageUrl,
                            contentDescription = uiState.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    uiState.loading -> {
                        CircularProgressIndicator(color = ComposeAppTheme.colors.grey)
                    }
                    else -> {
                        Text(
                            text = "?",
                            style = ComposeAppTheme.typography.title1,
                            color = ComposeAppTheme.colors.steel20
                        )
                    }
                }
            }

            // 名称与编号
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                body_leah(text = uiState.name)
                VSpacer(4.dp)
                subhead2_grey(text = "#${uiState.tokenId}")
            }
            VSpacer(16.dp)

            // 介绍
            if (uiState.description != null) {
                HeaderText(text = stringResource(R.string.Nft_Description))
                body_grey(
                    text = uiState.description!!,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                VSpacer(16.dp)
            }

            // 交易记录
            HeaderText(text = stringResource(R.string.Nft_Events))
            if (uiState.events.isEmpty()) {
                subhead2_grey(
                    text = stringResource(R.string.Nft_NoEvents),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            } else {
                uiState.events.forEach { event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        body_leah(
                            text = event.type,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            event.amount?.let { amount ->
                                Text(
                                    text = amount,
                                    style = ComposeAppTheme.typography.subhead,
                                    color = ComposeAppTheme.colors.leah
                                )
                            }
                            event.date?.let { date ->
                                caption_grey(text = date)
                            }
                        }
                    }
                }
            }
            VSpacer(32.dp)
        }
    }
}
