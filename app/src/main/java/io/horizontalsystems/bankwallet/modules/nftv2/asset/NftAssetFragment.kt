package io.horizontalsystems.bankwallet.modules.nftv2.asset

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.nftv2.send.SendNftFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.core.helpers.HudHelper
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
    val view = LocalView.current

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
                    .heightIn(min = 300.dp)
                    .background(ComposeAppTheme.colors.lawrence),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.imageUrl != null -> {
                        AsyncImage(
                            model = uiState.imageUrl,
                            contentDescription = uiState.name,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                    uiState.loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(60.dp),
                            color = ComposeAppTheme.colors.grey
                        )
                    }
                    else -> {
                        Text(
                            text = "?",
                            style = ComposeAppTheme.typography.title1,
                            color = ComposeAppTheme.colors.steel20,
                            modifier = Modifier.padding(60.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                body_leah(text = uiState.name)
                VSpacer(16.dp)

                NftInfoRow(stringResource(R.string.Nft_Collection), uiState.collectionName)
                NftInfoRow(stringResource(R.string.Nft_TokenId), "#${uiState.tokenId}")
                NftInfoRow(
                    stringResource(R.string.Nft_Contract),
                    uiState.contractAddress.shorten(),
                    onClick = {
                        TextHelper.copyText(uiState.contractAddress)
                        HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                    }
                )
                NftInfoRow(stringResource(R.string.Nft_Standard), uiState.nftType.name)
                if (uiState.balance > 1) {
                    NftInfoRow(stringResource(R.string.Nft_Owned), uiState.balance.toString())
                }
            }
        }
    }
}

@Composable
private fun NftInfoRow(
    title: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        subhead2_grey(text = title, modifier = Modifier.weight(1f))
        if (onClick != null) {
            androidx.compose.foundation.text.ClickableText(
                text = androidx.compose.ui.text.AnnotatedString(value),
                style = ComposeAppTheme.typography.subhead.copy(color = ComposeAppTheme.colors.leah),
                onClick = { onClick() }
            )
        } else {
            Text(
                text = value,
                style = ComposeAppTheme.typography.subhead,
                color = ComposeAppTheme.colors.leah
            )
        }
    }
}
