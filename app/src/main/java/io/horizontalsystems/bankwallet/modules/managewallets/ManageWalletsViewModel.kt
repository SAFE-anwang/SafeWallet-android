package io.horizontalsystems.bankwallet.modules.managewallets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.CoinViewItem
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.launch
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.bankwallet.modules.transactions.Filter
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.FullCoin
import io.reactivex.disposables.CompositeDisposable

class ManageWalletsViewModel(
    private val service: ManageWalletsService,
    private val clearables: List<Clearable>
) : ViewModel() {

    val filterBlockchainsLiveData = MutableLiveData<List<Filter<Blockchain?>>>()

    private var disposables = CompositeDisposable()
    val viewItemsLiveData = MutableLiveData<List<CoinViewItem<Token>>>()

    init {
        viewModelScope.launch {
            service.itemsFlow.collect {
                sync(it)
            }
        }
        updateFilterBlockchains(service.selectedBlockchain)
    }

    private fun sync(items: List<ManageWalletsService.Item>) {
        // DOGE 币不支持49、84、86
        val viewItems = items.map { viewItem(it) }.filter { !(it.title == "DOGE" && (it.label == "BIP49" || it.label == "BIP84" || it.label == "BIP86")) }
        viewItemsLiveData.postValue(viewItems)
    }


    private fun viewItem(
        item: ManageWalletsService.Item,
    ) = CoinViewItem(
        item = item.token,
        imageSource = ImageSource.Remote(item.token.coin.imageUrl, item.token.iconPlaceholder),
        title = item.token.coin.code,
        subtitle = item.token.coin.name,
        enabled = item.enabled,
        hasInfo = item.hasInfo,
        label = item.token.badge
    )

    fun enable(token: Token) {
        service.enable(token)
    }

    fun disable(token: Token) {
        service.disable(token)
    }

    fun updateFilter(filter: String) {
        service.setFilter(filter)
    }

    val addTokenEnabled: Boolean
        get() = service.accountType?.canAddTokens ?: false

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
    }

    fun onEnterFilterBlockchain(filterBlockchain: Filter<Blockchain?>) {
        service.setFilter(filterBlockchain.item)

        updateFilterBlockchains(filterBlockchain.item)
    }

    private fun updateFilterBlockchains(select: Blockchain?) {
        val filterBlockchains = service.blockchains.map {
            Filter(it, it == select)
        }
        filterBlockchainsLiveData.postValue(filterBlockchains)
    }

    data class BirthdayHeightViewItem(
        val blockchainIcon: ImageSource,
        val blockchainName: String,
        val birthdayHeight: String
    )
}
