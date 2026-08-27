package io.horizontalsystems.bankwallet.modules.nftv2.send

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.send.evm.confirmation.SendEvmConfirmationFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.nftkit.models.NftType
import kotlinx.parcelize.Parcelize

class SendNftFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            SendNftScreen(navController, input)
        }
    }

    @Parcelize
    data class Input(
        val blockchainType: BlockchainType,
        val contractAddress: String,
        val tokenId: String,
        val collectionName: String,
        val imageUrl: String?,
        val nftTypeName: String,
    ) : Parcelable {
        val nftType: NftType
            get() = try {
                NftType.valueOf(nftTypeName)
            } catch (e: Throwable) {
                NftType.Eip721
            }
    }
}

@Composable
private fun SendNftScreen(
    navController: NavController,
    input: SendNftFragment.Input
) {
    val viewModel = viewModel<SendNftViewModel>(
        factory = SendNftViewModel.Factory(
            input.blockchainType,
            input.contractAddress,
            input.tokenId,
            input.nftType
        )
    )
    val uiState = viewModel.uiState

    HSScaffold(
        title = stringResource(R.string.Nft_Send_Title),
        onBack = { navController.popBackStack() },
        bottomBar = {
            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                title = stringResource(R.string.Send_DialogProceed),
                enabled = uiState.proceedEnabled,
                onClick = {
                    val transactionData = viewModel.buildTransactionData() ?: return@ButtonPrimaryYellow
                    val sendData = SendEvmData(
                        transactionData = transactionData,
                        additionalInfo = SendEvmData.AdditionalInfo.Send(
                            SendEvmData.SendInfo(
                                nftShortMeta = SendEvmData.NftShortMeta(
                                    nftName = "${input.collectionName} #${input.tokenId}",
                                    previewImageUrl = input.imageUrl
                                )
                            )
                        )
                    )
                    navController.slideFromRight(
                        R.id.sendEvmConfirmationFragment,
                        SendEvmConfirmationFragment.Input(
                            sendData,
                            input.blockchainType,
                            R.id.nftCollectionFragment
                        )
                    )
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // NFT 信息摘要
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ComposeAppTheme.colors.lawrence)
                    .padding(12.dp)
            ) {
                if (input.imageUrl != null) {
                    AsyncImage(
                        model = input.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                VSpacer(0.dp)
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    body_leah(text = "${input.collectionName} #${input.tokenId}")
                    subhead2_grey(text = input.nftTypeName)
                }
            }

            VSpacer(24.dp)

            // 接收地址输入
            HSAddressInput(
                modifier = Modifier.fillMaxWidth(),
                tokenQuery = viewModel.tokenQuery,
                coinCode = viewModel.coinCode,
                hint = stringResource(R.string.Send_EnterAddress),
                navController = navController,
                onValueChange = viewModel::onAddressChange
            )
        }
    }
}
