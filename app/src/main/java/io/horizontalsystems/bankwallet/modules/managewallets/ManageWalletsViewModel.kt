package io.horizontalsystems.bankwallet.modules.managewallets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.label
import io.horizontalsystems.bankwallet.entities.supportedPlatforms
import io.horizontalsystems.bankwallet.modules.managewallets.ManageWalletsService.ItemState.Supported
import io.horizontalsystems.bankwallet.modules.managewallets.ManageWalletsService.ItemState.Unsupported
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.restore.restoreblockchains.CoinViewItem
import io.horizontalsystems.bankwallet.modules.restore.restoreblockchains.CoinViewItemState
import io.horizontalsystems.marketkit.models.FullCoin
import io.reactivex.disposables.CompositeDisposable

class ManageWalletsViewModel(
    private val service: ManageWalletsService,
    private val clearables: List<Clearable>
) : ViewModel() {

    val viewItemsLiveData = MutableLiveData<List<CoinViewItem>>()
    val disableCoinLiveData = MutableLiveData<String>()

    private var disposables = CompositeDisposable()

    init {
        service.itemsObservable
            .subscribeIO { sync(it) }
            .let { disposables.add(it) }

        service.cancelEnableCoinObservable
            .subscribeIO { disableCoinLiveData.postValue(it.uid) }
            .let { disposables.add(it) }

        sync(service.items)
    }

    private fun sync(items: List<ManageWalletsService.Item>) {
        val viewItems = items.map { viewItem(it) }
        //自定义排序
        safeSort(viewItems as ArrayList)
        viewItemsLiveData.postValue(viewItems)
    }


    private fun safeSort(items: ArrayList<CoinViewItem>): List<CoinViewItem> {
        var safe: CoinViewItem? = null
        var safeErc20: CoinViewItem? = null
        var bsvErc20: CoinViewItem? = null
        items.forEach {
            if (it.uid == "safe-coin") {
                safe = it
            } else if (it.uid == "custom_safe-erc20-SAFE") {
                safeErc20 = it
            } else if (it.uid == "custom_safe-bep20-SAFE") {
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
    ): CoinViewItem {
        val supportedPlatforms = item.fullCoin.supportedPlatforms
        val label = supportedPlatforms.singleOrNull()?.coinType?.label
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
            item.fullCoin.coin.uid,
            image,
            item.fullCoin.coin.name,
            item.fullCoin.coin.code,
            state,
            label,
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

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposables.clear()
    }

}
