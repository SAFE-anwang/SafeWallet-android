package io.horizontalsystems.bankwallet.modules.safe4.safeprice

import com.github.mikephil.charting.data.CandleEntry
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.net.SafeApiKeyService
import io.horizontalsystems.ethereumkit.models.Chain
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.CopyOnWriteArrayList

class SRC20InfoService {

    private val TAG = "KChartService"

    private val disposables = CompositeDisposable()

    private val priceDatas = CopyOnWriteArrayList<MarketPrice>()
    private val itemsSubject = PublishSubject.create<List<MarketPrice>>()
    val itemsObservable: Observable<List<MarketPrice>> get() = itemsSubject

    val safeApiKeyService by lazy { SafeApiKeyService(Chain.SafeFour.isSafe4TestNetId) }

    fun getPrice() {
        safeApiKeyService.getPrice()
            .subscribeOn(Schedulers.io())
            .subscribe({
                priceDatas.addAll(it)
                itemsSubject.onNext(priceDatas)
            }, {
                Log.e(TAG, "error=$it")
            }).let {
                disposables.add(it)
            }
    }
}