package io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorMultipleDialog
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorViewItem
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.disposables.Disposable

class CoinTokensViewModel(
    private val service: CoinTokensService,
    private val accountType: AccountType?
) : ViewModel() {
    val openSelectorEvent = SingleLiveEvent<BottomSheetSelectorMultipleDialog.Config>()

    private var currentRequest: CoinTokensService.Request? = null
    private val disposable: Disposable

    init {
        disposable = service.requestObservable
            .subscribeIO {
                handle(it)
            }
    }

    private fun handle(request: CoinTokensService.Request) {
        currentRequest = request
        val fullCoin = request.fullCoin
        // 过滤不支持的币
        val currentTokens = filterSupportTokens(request.currentTokens)
        val tokens = filterSupportTokens(fullCoin.supportedTokens)
        val selectedTokenIndexes = if (request.currentTokens.isEmpty()) listOf(0) else currentTokens.map { tokens.indexOf(it) }
        val imageSource = if (fullCoin.coin.uid == "safe-coin") {
            ImageSource.Local(R.drawable.logo_safe_24)
        } else {
            ImageSource.Remote(fullCoin.coin.iconUrl, fullCoin.iconPlaceholder)
        }

        val config = BottomSheetSelectorMultipleDialog.Config(
            icon = imageSource,
            title = fullCoin.coin.code,
            description = if (tokens.size > 1) Translator.getString(R.string.CoinPlatformsSelector_Description) else null,
            selectedIndexes = selectedTokenIndexes,
            allowEmpty = request.allowEmpty,
            viewItems = tokens.map { token ->
                BottomSheetSelectorViewItem(
                    title = token.protocolInfo,
                    subtitle = token.typeInfo,
                    copyableString = token.copyableTypeInfo,
                    icon = token.blockchainType.imageUrl
                )
            }
        )
        openSelectorEvent.postValue(config)
    }

    fun onSelect(indexes: List<Int>) {
        currentRequest?.let { currentRequest ->
            val platforms = filterSupportTokens(currentRequest.fullCoin.supportedTokens)
            service.select(indexes.map { platforms[it] }, currentRequest.fullCoin.coin)
        }
    }

    fun onCancelSelect() {
        currentRequest?.let { currentRequest ->
            service.cancel(currentRequest.fullCoin)
        }
    }

    private fun filterSupportTokens(tokens: List<Token>):List<Token> {
        return tokens.filter { token ->
            if (accountType is AccountType.EvmPrivateKey) {
                token.blockchainType !is BlockchainType.Safe
            } else {
                true
            }
        }
    }

}
