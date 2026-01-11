package io.horizontalsystems.bankwallet.modules.safe4.kchart

import com.github.mikephil.charting.data.CandleEntry
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.net.SafeApiKeyService
import io.horizontalsystems.ethereumkit.models.Chain
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.CopyOnWriteArrayList

class KChartService {

    private val TAG = "KChartService"

    private val disposables = CompositeDisposable()

    private val kChartDatas = CopyOnWriteArrayList<KChartData>()
    private val itemsSubject = PublishSubject.create<List<KChartData>>()
    val itemsObservable: Observable<List<KChartData>> get() = itemsSubject

    val safeApiKeyService by lazy { SafeApiKeyService(Chain.SafeFour.isSafe4TestNetId) }

    fun getKChartData(token0: String, token1: String, interval: String) {
        safeApiKeyService.getKChart(token0, token1, interval)
            .subscribeOn(Schedulers.io())
            /*.map {
                it.mapIndexed { index, data ->
                    CandleEntry(
                        data.timestamp.toFloat(),
                        data.high.toFloat(),
                        data.low.toFloat(),
                        data.open.toFloat(),
                        data.close.toFloat()
                    )
                }
            }*/
            .subscribe({
                kChartDatas.addAll(it)
                itemsSubject.onNext(kChartDatas)
            }, {
                Log.e(TAG, "error=$it")
            }).let {
                disposables.add(it)
            }
    }
}