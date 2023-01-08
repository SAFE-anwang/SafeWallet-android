package io.horizontalsystems.bankwallet.modules.safe4.linelock

import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.ISendSafeAdapter
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.hodler.LockTimeInterval
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal

class LineLockSendInteractor(private val adapter: ISendSafeAdapter) : SendModule.ISendSafeInteractor {
    private val disposables = CompositeDisposable()

    var delegate: SendModule.ISendSafeInteractorDelegate? = null

    override fun fetchAvailableBalance(address: String?) {
        Single.just(adapter.availableBalanceSafe(address))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ availableBalance ->
                delegate?.didFetchAvailableBalance(availableBalance)
            }, {

            })
            .let { disposables.add(it) }
    }

    override fun fetchMinimumAmount(address: String?): BigDecimal? {
        return adapter.minimumSendAmountSafe(address)
    }

    override fun validate(address: String) {
        adapter.validateSafe(address)
    }

    override fun fetchFee(amount: BigDecimal, address: String?) {
        if (amount.toInt() == 0)    return
        val feeSafe = adapter.feeSafe(amount, address) ?: return
        Single.just(feeSafe)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ fee ->
                fee?.let {
                    delegate?.didFetchFee(fee)
                }
            }, {

            })
            .let { disposables.add(it) }
    }

    override fun send(amount: BigDecimal, address: String, logger: AppLogger, lockTimeInterval: LockTimeInterval ?, reverseHex: String ?): Single<Unit> {
        return adapter.sendSafe( amount, address, logger , lockTimeInterval, reverseHex)
    }

    override fun clear() {
        disposables.clear()
    }

}
