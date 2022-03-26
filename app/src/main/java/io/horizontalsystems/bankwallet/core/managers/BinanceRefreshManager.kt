package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.binancechainkit.BinanceChainKit
import io.horizontalsystems.binancechainkit.core.IStorage
import io.horizontalsystems.binancechainkit.storage.KitDatabase
import io.horizontalsystems.binancechainkit.storage.Storage
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class BinanceRefreshManager(
    private val context: Context,
    private val accountManager: IAccountManager,
    private val binanceKitManager: BinanceKitManager,
    private val networkType: BinanceChainKit.NetworkType = BinanceChainKit.NetworkType.MainNet
): Clearable {

    private val disposable = CompositeDisposable()

    private val binanceLaunchTime: Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { set(2019, 0, 1, 0, 0, 0) }.time.time

    private var storage: IStorage? = null

    init {
        accountManager.activeAccountObservable
            .subscribeIO {
                binanceKitManager.binanceKit?.let {
                    startRefreshBinance()
                }
            }.let {
                disposable.add(it)
            }
    }

    fun startRefreshBinance() {
        accountManager.activeAccount?.let {
            val database = KitDatabase.create(context,
                "Binance-$networkType-${it.id}"
            )
            storage = Storage(database)
        }
        if (storage == null) {
            return
        }
        val currentTime = Date().time - 60_000
        GlobalScope.launch(Dispatchers.IO) {
            while ((storage?.syncState?.transactionSyncedUntilTime ?: binanceLaunchTime) < currentTime
                && binanceKitManager.binanceKit != null) {
                binanceKitManager.binanceKit?.refresh()
                delay(4000)
            }
            storage = null
        }

    }

    override fun clear() {
        disposable.clear()
    }
}