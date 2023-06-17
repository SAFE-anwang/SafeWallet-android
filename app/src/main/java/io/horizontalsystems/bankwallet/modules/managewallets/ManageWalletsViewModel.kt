package io.horizontalsystems.bankwallet.modules.managewallets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.ConfiguredToken
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.CoinViewItem
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.bankwallet.modules.transactions.Filter
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.FullCoin
import io.reactivex.disposables.CompositeDisposable

class ManageWalletsViewModel(
    private val service: ManageWalletsService,
    private val clearables: List<Clearable>
) : ViewModel() {

    val viewItemsLiveData = MutableLiveData<List<CoinViewItem<ConfiguredToken>>>()
    val filterBlockchainsLiveData = MutableLiveData<List<Filter<Blockchain?>>>()

    private var disposables = CompositeDisposable()

    init {
        service.itemsObservable
            .subscribeIO { sync(it) }
            .let { disposables.add(it) }

        sync(service.items)

        updateFilterBlockchains(service.selectedBlockchain)
    }

    private fun sync(items: List<ManageWalletsService.Item>) {
        val viewItems = items.map { viewItem(it) }
        //自定义排序
//        safeSort(viewItems as ArrayList)
        viewItemsLiveData.postValue(viewItems)
    }


    private fun safeSort(items: ArrayList<CoinViewItem<ConfiguredToken>>): List<CoinViewItem<ConfiguredToken>> {
        var safe: CoinViewItem<ConfiguredToken>? = null
        var safeErc20: CoinViewItem<ConfiguredToken>? = null
        var bsvErc20: CoinViewItem<ConfiguredToken>? = null
        items.forEach {
            if (it.item.token.coin.uid == "safe-coin") {
                safe = it
            } else if (it.item.token.coin.uid == "custom_safe-erc20-SAFE") {
                safeErc20 = it
            } else if (it.item.token.coin.uid == "custom_safe-bep20-SAFE") {
                bsvErc20 = it
            }
        }
        if (bsvErc20 != null) {
            items.remove(bsvErc20)
            items.add(0, bsvErc20!!)
        }
        if (safeErc20 != null) {
            items.remove(safeErc20)
            items.add(0, safeErc20!!)
        }
        if (safe != null) {
            items.remove(safe)
            items.add(0, safe!!)
        }
        return items
    }

    private fun viewItem(
        item: ManageWalletsService.Item,
    ) = CoinViewItem(
        item = item.configuredToken,
        imageSource = ImageSource.Remote(item.configuredToken.token.coin.imageUrl, item.configuredToken.token.iconPlaceholder),
        title = item.configuredToken.token.coin.code,
        subtitle = item.configuredToken.token.coin.name,
        enabled = item.enabled,
        hasInfo = item.hasInfo,
        label = item.configuredToken.badge
    )

    fun enable(configuredToken: ConfiguredToken) {
        service.enable(configuredToken)
    }

    fun disable(configuredToken: ConfiguredToken) {
        service.disable(configuredToken)
    }

    fun updateFilter(filter: String) {
        service.setFilter(filter)
    }

    val addTokenEnabled: Boolean
        get() = service.accountType?.canAddTokens ?: false

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposables.clear()
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
