package io.horizontalsystems.bankwallet.modules.watchaddress.selectblockchains

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.core.description
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.CoinViewItem
import io.horizontalsystems.bankwallet.modules.watchaddress.WatchAddressService
import io.horizontalsystems.marketkit.SafeExtend.isSafeCoin
import io.horizontalsystems.marketkit.models.Token

class SelectBlockchainsViewModel(
    private val accountType: AccountType,
    private val accountName: String?,
    private val service: WatchAddressService
) : ViewModelUiState<SelectBlockchainsUiState>() {

    private var title: Int = R.string.Watch_Select_Blockchains
    private var coinViewItems = listOf<CoinViewItem<Token>>()
    private var selectedCoins = setOf<Token>()
    private var accountCreated = false

    init {
        val tokens = service.tokens(accountType)

        when (accountType) {
            is AccountType.SolanaAddress,
            is AccountType.TronAddress,
            is AccountType.BitcoinAddress,
            is AccountType.TonAddress,
            is AccountType.Cex,
            is AccountType.Mnemonic,
            is AccountType.PrivateKey,
            is AccountType.EvmPrivateKey -> Unit // N/A
            is AccountType.EvmAddress -> {
                title = R.string.Watch_Select_Blockchains
                coinViewItems = tokens.map {
                    coinViewItemForBlockchain(it)
                }
            }

            is AccountType.HdExtendedKey -> {
                title = R.string.Watch_Select_Coins
                coinViewItems = tokens.map {
                    coinViewItemForToken(it, label = it.badge)
                }
            }
        }

        emitState()
    }

    override fun createState() = SelectBlockchainsUiState(
        title = title,
        coinViewItems = coinViewItems,
        submitButtonEnabled = selectedCoins.isNotEmpty(),
        accountCreated = accountCreated
    )

    private fun coinViewItemForBlockchain(token: Token): CoinViewItem<Token> {
        val blockchain = token.blockchain
        return CoinViewItem(
            item = token,
            imageSource = getImageSource(token, R.drawable.ic_platform_placeholder_32),
            title = blockchain.name,
            subtitle = blockchain.description,
            enabled = false
        )
    }

    private fun coinViewItemForToken(token: Token, label: String?): CoinViewItem<Token> {
        return CoinViewItem(
            item = token,
            imageSource = getImageSource(token),
            title = token.fullCoin.coin.code,
            subtitle = token.fullCoin.coin.name,
            enabled = false,
            label = label
        )
    }

    private fun getImageSource(token: Token, placeHolder: Int = R.drawable.coin_placeholder): ImageSource {
        return if (token.coin.isSafeCoin()) {
            ImageSource.Local(R.drawable.logo_safe_24)
        } else {
            ImageSource.Remote(token.fullCoin.coin.imageUrl, placeHolder)
        }
    }

    fun onToggle(token: Token) {
        selectedCoins = if (selectedCoins.contains(token))
            selectedCoins.toMutableSet().also { it.remove(token) }
        else
            selectedCoins.toMutableSet().also { it.add(token) }

        coinViewItems = coinViewItems.map { viewItem ->
            viewItem.copy(enabled = selectedCoins.contains(viewItem.item))
        }

        emitState()
    }

    fun onClickWatch() {
        service.watchTokens(accountType, selectedCoins.toList(), accountName)
        accountCreated = true
        emitState()
    }

}

data class SelectBlockchainsUiState(
    val title: Int,
    val coinViewItems: List<CoinViewItem<Token>>,
    val submitButtonEnabled: Boolean,
    val accountCreated: Boolean
)
