package io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.marketkit.models.Blockchain
import io.reactivex.BackpressureStrategy
import io.reactivex.disposables.CompositeDisposable

class RestoreBlockchainsViewModel(
    private val service: RestoreBlockchainsService,
    private val clearables: List<Clearable>
) : ViewModel() {

    val viewItemsLiveData = MutableLiveData<List<CoinViewItem<Blockchain>>>()
    val disableBlockchainLiveData = MutableLiveData<String>()
    val successLiveEvent = SingleLiveEvent<Boolean?>()
    var restored by mutableStateOf(false)
        private set
    val restoreEnabledLiveData: LiveData<Boolean>
        get() = service.canRestore.toFlowable(BackpressureStrategy.DROP).toLiveData()

    private var disposables = CompositeDisposable()

    init {
        service.itemsObservable
            .subscribeIO { sync(it) }
            .let { disposables.add(it) }

        service.cancelEnableBlockchainObservable
            .subscribeIO { disableBlockchainLiveData.postValue(it.uid) }
            .let { disposables.add(it) }

        sync(service.items)
    }

    private fun sync(items: List<RestoreBlockchainsService.Item>) {
        val viewItems = items.map { viewItem(it) }
        viewItemsLiveData.postValue(viewItems)
    }

    private fun viewItem(
        item: RestoreBlockchainsService.Item
    ):CoinViewItem<Blockchain> {
        val imageSource = if (item.blockchain.uid == "safe-coin") {
            ImageSource.Local(R.drawable.safe)
        } else {
            ImageSource.Remote(
                if (item.blockchain.uid == "dogecoin") {
                    item.blockchain.type.dogeImageUrl
                } else {
                    item.blockchain.type.imageUrl
                },
                R.drawable.ic_platform_placeholder_32
            )
        }
        return CoinViewItem(
            item = item.blockchain,
            imageSource = imageSource,
            title = item.blockchain.name,
            subtitle = if (item.blockchain.uid == "dogecoin") "" else item.blockchain.description,
            enabled = item.enabled,
            hasSettings = item.hasSettings,
            hasInfo = false
        )
    }

    fun enable(blockchain: Blockchain, purpose: Int? = null) {
        service.enable(blockchain, purpose)
    }

    fun disable(blockchain: Blockchain) {
        service.disable(blockchain)
    }

    fun onClickSettings(blockchain: Blockchain) {
        service.configure(blockchain)
    }

    fun onRestore() {
        service.restore()
        restored = true
        successLiveEvent.call()
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposables.clear()
    }
}
