package io.horizontalsystems.bankwallet.modules.send

import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.AppLogger
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class SendInteractor : SendModule.ISendInteractor {
    private var disposables = CompositeDisposable()

    override lateinit var delegate: SendModule.ISendInteractorDelegate

    override fun send(sendSingle: Single<Unit>, logger: AppLogger) {
        sendSingle.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.e("longwen", "send success")
                    logger.info("success")

                    delegate.didSend()
                }, { error ->
                    Log.e("longwen", "send error")
                    logger.warning("failed", error)

                    delegate.didFailToSend(error)
                }).let {
                    disposables.add(it)
                }
    }

    override fun clear() {
        disposables.clear()
    }

}
