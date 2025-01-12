package io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.description
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.marketkit.SafeExtend.isSafeCoin
import io.horizontalsystems.marketkit.models.Blockchain
import io.reactivex.BackpressureStrategy
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

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

    init {
        viewModelScope.launch {
            service.itemsObservable.asFlow().collect {
                sync(it)
            }
        }

        viewModelScope.launch {
            service.cancelEnableBlockchainObservable.asFlow().collect {
                disableBlockchainLiveData.postValue(it.uid)
            }
        }

        sync(service.items)
    }

    private fun sync(items: List<RestoreBlockchainsService.Item>) {
        val viewItems = items.map { viewItem(it) }
        viewItemsLiveData.postValue(viewItems)
    }

    private fun viewItem(
        item: RestoreBlockchainsService.Item
    ):CoinViewItem<Blockchain> {
        val imageSource = if (item.blockchain.uid.isSafeCoin()) {
            ImageSource.Local(R.drawable.safe)
        } else {
            ImageSource.Remote(
                item.blockchain.type.imageUrl,
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
        service.enable(blockchain)
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
    }
}
