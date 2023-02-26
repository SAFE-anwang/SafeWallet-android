package io.horizontalsystems.bankwallet.modules.settings.security.fallbackblock

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.adapters.SafeAdapter
import io.horizontalsystems.bankwallet.core.managers.AdapterManager
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.reactivex.Observable
import io.reactivex.Observable.create
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class FallbackBlockViewModel(
    val walletManager: IWalletManager,
    val accountManager: IAccountManager,
    val adapterManager: IAdapterManager
) : ViewModel() {

    private var disposables: CompositeDisposable = CompositeDisposable()
    val itemsTime = mutableListOf<FallbackTimeViewItem>()
    val items = mutableListOf<FallbackViewItem>()

    init {
        itemsTime.add(FallbackTimeViewItem(R.string.fallback_block_time_1, 2023,2))
//        itemsTime.add(FallbackTimeViewItem(R.string.fallback_block_time_2, 2023,1))
        itemsTime.add(FallbackTimeViewItem(R.string.fallback_block_time_3, 2022,12))
//        itemsTime.add(FallbackTimeViewItem(R.string.fallback_block_time_4, 2022,11))
        itemsTime.add(FallbackTimeViewItem(R.string.fallback_block_time_5, 2022,10))
//        itemsTime.add(FallbackTimeViewItem(R.string.fallback_block_time_6, 2022,9))
        itemsTime.add(FallbackTimeViewItem(R.string.fallback_block_time_7, 2022,8))

        items.add(FallbackViewItem(Blockchain(BlockchainType.Safe, App.instance.getString(R.string.fallback_block_type, "SAFE"), null)))
    }

    override fun onCleared() {
        disposables.clear()
    }

    fun fallback(block: Blockchain, year: Int, month: Int) {
        disposables.add(Observable.create(object : ObservableOnSubscribe<String> {
            override fun subscribe(emitter: ObservableEmitter<String>) {
                walletManager.activeWallets.find { it.coin.uid == "safe-coin" && it.token.blockchainType == BlockchainType.Safe }?.let {
                    try {
                        adapterManager.getAdapterForWallet(it)?.let {
                            (it as SafeAdapter).fallbackBlock(year, month)
                        }
                        App.adapterManager.preloadAdapters()
                    }catch (e: Exception) {

                    }

                }
            }

        }).subscribeOn(Schedulers.io()).subscribe()
        )

    }

    data class FallbackViewItem(
        val blockchain: Blockchain
    )

    data class FallbackTimeViewItem(
        val nameRes: Int,
        val year: Int,
        val month: Int
    )
}

