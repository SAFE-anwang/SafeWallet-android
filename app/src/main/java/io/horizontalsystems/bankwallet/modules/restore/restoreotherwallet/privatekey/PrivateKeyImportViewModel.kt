package io.horizontalsystems.bankwallet.modules.restore.restoreotherwallet.privatekey

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.restore.restoreblockchains.RestoreBlockchainsModule
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.BackpressureStrategy

class PrivateKeyImportViewModel(
    private val service: RestorePrivateKeyService
): ViewModel() {

    val restoreEnabledLiveData: LiveData<Boolean>
        get() = LiveDataReactiveStreams.fromPublisher(
            service.canRestore.toFlowable(BackpressureStrategy.DROP)
        )

    val successLiveEvent = SingleLiveEvent<Unit>()

    fun onRestore(privateKey: String) {
        val accountType = AccountType.PrivateKey(privateKey.toByteArray())
        service.restore(accountType)
        successLiveEvent.call()
    }

    fun enable(type: String) {
        val blockchain = when(type) {
            "ETH" -> RestoreBlockchainsModule.Blockchain.Ethereum
            "BTC" -> RestoreBlockchainsModule.Blockchain.Bitcoin
            else -> {
                RestoreBlockchainsModule.Blockchain.Ethereum
            }
        }
        service.enable(blockchain)
    }

    fun disable(type: String) {
        val blockchain = when(type) {
            "ETH" -> RestoreBlockchainsModule.Blockchain.Ethereum
            "BTC" -> RestoreBlockchainsModule.Blockchain.Bitcoin
            else -> {
                RestoreBlockchainsModule.Blockchain.Ethereum
            }
        }
        service.disable(blockchain)
    }

}