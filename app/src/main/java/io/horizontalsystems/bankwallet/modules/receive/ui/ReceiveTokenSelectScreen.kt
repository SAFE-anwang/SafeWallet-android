package io.horizontalsystems.bankwallet.modules.receive.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.imagePlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.CoinForReceiveType
import io.horizontalsystems.bankwallet.modules.receive.viewmodels.ReceiveTokenSelectViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.SearchBar
import io.horizontalsystems.bankwallet.ui.compose.components.SectionUniversalItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.marketkit.SafeExtend.isSafeCoin
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ReceiveTokenSelectScreen(
    activeAccount: Account,
    onMultipleAddressesClick: (String) -> Unit,
    onMultipleDerivationsClick: (String) -> Unit,
    onMultipleBlockchainsClick: (String) -> Unit,
    onCoinClick: (Wallet) -> Unit,
    onBackPress: () -> Unit,
) {
    val viewModel = viewModel<ReceiveTokenSelectViewModel>(
        factory = ReceiveTokenSelectViewModel.Factory(activeAccount)
    )
    val fullCoins = viewModel.uiState.fullCoins
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            SearchBar(
                title = stringResource(R.string.Balance_Receive),
                searchHintText = stringResource(R.string.Balance_ReceiveHint_Search),
                menuItems = listOf(),
                onClose = onBackPress,
                onSearchTextChanged = { text ->
                    viewModel.updateFilter(text)
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(contentPadding = paddingValues,
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ComposeAppTheme.colors.lawrence)) {
            item {
                VSpacer(12.dp)
            }
            itemsIndexed(fullCoins) { index, fullCoin ->
                val coin = fullCoin.coin
                val lastItem = index == fullCoins.size - 1
                SectionUniversalItem(borderTop = true, borderBottom = lastItem) {
                    ReceiveCoin(
                        coinName = coin.name,
                        coinCode = coin.code,
                        uid = coin.uid,
                        coinIconUrl = coin.imageUrl,
                        coinIconPlaceholder = coin.imagePlaceholder,
                        onClick = {
                            coroutineScope.launch {
                                when (val coinActiveWalletsType = viewModel.getCoinForReceiveType(fullCoin)) {
                                    CoinForReceiveType.MultipleAddressTypes -> {
                                        onMultipleAddressesClick.invoke(coin.uid)
                                    }

                                    CoinForReceiveType.MultipleDerivations -> {
                                        onMultipleDerivationsClick.invoke(coin.uid)
                                    }

                                    CoinForReceiveType.MultipleBlockchains -> {
                                        onMultipleBlockchainsClick.invoke(coin.uid)
                                    }

                                    is CoinForReceiveType.Single -> {
                                        onCoinClick.invoke(coinActiveWalletsType.wallet)
                                    }

                                    null -> Unit
                                }
                            }
                        }
                    )
                }
            }
            item {
                VSpacer(32.dp)
            }
        }
    }
}

@Composable
fun ReceiveCoin(
    coinName: String,
    coinCode: String,
    uid: String,
    coinIconUrl: String,
    coinIconPlaceholder: Int,
    onClick: (() -> Unit)? = null
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = onClick
    ) {
        if (uid.isSafeCoin()) {
            Image(painter = painterResource(id = R.drawable.logo_safe_24),
                    contentDescription = null,
                    modifier = Modifier
                            .padding(end = 16.dp)
                            .size(32.dp)
            )
        } else {
            CoinImage(
                    iconUrl = coinIconUrl,
                    placeholder = coinIconPlaceholder,
                    modifier = Modifier
                            .padding(end = 16.dp)
                            .size(32.dp)
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                body_leah(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
                    text = coinCode,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            VSpacer(3.dp)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                subhead2_grey(
                    text = coinName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
