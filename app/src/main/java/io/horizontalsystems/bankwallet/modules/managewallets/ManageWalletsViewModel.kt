package io.horizontalsystems.bankwallet.modules.managewallets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.managewallets.ManageWalletsService.ItemState.Supported
import io.horizontalsystems.bankwallet.modules.managewallets.ManageWalletsService.ItemState.Unsupported
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.CoinViewItem
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.CoinViewItemState
import io.horizontalsystems.bankwallet.modules.transactions.Filter
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.FullCoin
import io.reactivex.disposables.CompositeDisposable

class ManageWalletsViewModel(
    private val service: ManageWalletsService,
    private val clearables: List<Clearable>
) : ViewModel() {

    val viewItemsLiveData = MutableLiveData<List<CoinViewItem<String>>>()
    val disableCoinLiveData = MutableLiveData<String>()
    val filterBlockchainsLiveData = MutableLiveData<List<Filter<Blockchain?>>>()

    private var disposables = CompositeDisposable()

    init {
        service.itemsObservable
            .subscribeIO { sync(it) }
            .let { disposables.add(it) }

        service.cancelEnableCoinObservable
            .subscribeIO { disableCoinLiveData.postValue(it.uid) }
            .let { disposables.add(it) }

        sync(service.items)

        updateFilterBlockchains(service.selectedBlockchain)
    }

    private fun sync(items: List<ManageWalletsService.Item>) {
        val viewItems = items.map { viewItem(it) }
        //自定义排序
        safeSort(viewItems as ArrayList)
        viewItemsLiveData.postValue(viewItems)
    }


    private fun safeSort(items: ArrayList<CoinViewItem<String>>): List<CoinViewItem<String>> {
        var safe: CoinViewItem<String>? = null
        var safeErc20: CoinViewItem<String>? = null
        var bsvErc20: CoinViewItem<String>? = null
        items.forEach {
            if (it.item == "safe-coin") {
                safe = it
            } else if (it.item == "custom_safe-erc20-SAFE") {
                safeErc20 = it
            } else if (it.item == "custom_safe-bep20-SAFE") {
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
    ): CoinViewItem<String> {
        val state = when (item.state) {
            is Supported -> CoinViewItemState.ToggleVisible(
                item.state.enabled,
                item.state.hasSettings
            )
            is Unsupported -> CoinViewItemState.ToggleHidden
        }
        val image = if (item.fullCoin.coin.uid == "safe-coin"
            || item.fullCoin.coin.uid == "custom_safe-erc20-SAFE"
            || item.fullCoin.coin.uid == "custom_safe-bep20-SAFE") {
            ImageSource.Local(R.drawable.logo_safe_24)
        } else {
            ImageSource.Remote(item.fullCoin.coin.iconUrl, item.fullCoin.iconPlaceholder)
        }
        return CoinViewItem(
            item = item.fullCoin.coin.uid,
            imageSource = image,
            title = item.fullCoin.coin.code,
            subtitle = item.fullCoin.coin.name,
            state = state,
        )
    }

    fun enable(fullCoin: FullCoin) {
        service.enable(fullCoin)
    }

    fun enable(uid: String) {
        service.enable(uid)
    }

    fun disable(uid: String) {
        service.disable(uid)
    }

    fun onClickSettings(uid: String) {
        service.configure(uid)
    }

    fun updateFilter(filter: String) {
        service.setFilter(filter)
    }

    val accountTypeDescription: String
        get() = service.accountType?.description ?: ""

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
}
