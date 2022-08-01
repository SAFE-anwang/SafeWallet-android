package io.horizontalsystems.bankwallet.modules.send.submodules.hodler

import androidx.lifecycle.ViewModel
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.hodler.HodlerData
import io.horizontalsystems.hodler.HodlerPlugin
import io.horizontalsystems.hodler.LockTimeInterval
import java.math.BigDecimal

class SendHodlerPresenter(
        val view: SendHodlerModule.IView,
        private val interactor: SendHodlerModule.IInteractor
) : ViewModel(), SendHodlerModule.IViewDelegate, SendHodlerModule.IHodlerModule {

    var moduleDelegate: SendHodlerModule.IHodlerModuleDelegate? = null

    private var lockTimeIntervals = arrayOf<LockTimeInterval?>()
    private var lockTimeIntervalSelected: LockTimeInterval? = null

    private var lockedValue: String? = null
    private var startMonth: String? = null
    private var intervalMonth: String? = null

    override fun onViewDidLoad() {
        lockTimeIntervals = arrayOf<LockTimeInterval?>(null) + interactor.getLockTimeIntervals()

        view.setSelectedLockTimeInterval(lockTimeIntervalSelected)
    }

    override fun onClickLockTimeInterval() {
        val items = lockTimeIntervals.map {
            SendHodlerModule.LockTimeIntervalViewItem(it, it == lockTimeIntervalSelected)
        }
        view.showLockTimeIntervalSelector(items)
    }

    override fun onSelectLockTimeInterval(position: Int) {
        lockTimeIntervalSelected = lockTimeIntervals[position]

        view.setSelectedLockTimeInterval(lockTimeIntervalSelected)

        moduleDelegate?.onUpdateLockTimeInterval(lockTimeIntervalSelected)
    }

    override fun onTextChangeLockedValue(value: String) {
        lockedValue = value
        moduleDelegate?.onUpdateLineLock(lockedValue, startMonth, intervalMonth)
    }

    override fun onTextChangeStartMonth(value: String) {
        startMonth = value
        moduleDelegate?.onUpdateLineLock(lockedValue, startMonth, intervalMonth)
    }

    override fun onTextChangeIntervalMonth(value: String) {
        intervalMonth = value
        moduleDelegate?.onUpdateLineLock(lockedValue, startMonth, intervalMonth)
    }

    override fun pluginData(): Map<Byte, IPluginData> {
        return lockTimeIntervalSelected?.let {
            mapOf(HodlerPlugin.id to HodlerData(it))
        } ?: mapOf()
    }

    override fun setAmount(amount: BigDecimal) {
        var lockedValue: String
        val startMonth: String
        val intervalMonth: String
        val outputSize: Int
        if (amount < BigDecimal(1)) { // 小于1 SAFE
            outputSize = 1
            lockedValue = amount.toPlainString()
            startMonth = "1"
            intervalMonth = "1"
        } else if (amount >= BigDecimal(1) &&amount < BigDecimal(120)) { // 1-120 SAFE
            outputSize = (amount / BigDecimal(1)).toInt()
            lockedValue = "1"
            startMonth = "1"
            intervalMonth = "1"
        } else if (amount >= BigDecimal(120) && amount < BigDecimal(1000)) { // 120-1000 SAFE
            outputSize = (amount / BigDecimal(120)).toInt()
            lockedValue = (amount / BigDecimal(outputSize)).toLong().toString()
            startMonth = "1"
            intervalMonth = "1"
        } else {
            outputSize = (amount / BigDecimal(1000)).toInt()    // 大于等于1000 SAFE
            lockedValue = (amount / BigDecimal(outputSize)).toLong().toString()
            startMonth = "12"
            intervalMonth = "1"
        }
        lockedValue = checkAmount(amount, BigDecimal(lockedValue)).toString()
        val totalAmount = BigDecimal(lockedValue) * BigDecimal(outputSize)
        Log.i("safe4", "totalAmount: $totalAmount")
        view.setLockedValue(lockedValue)
        view.setStartMonth(startMonth)
        view.setIntervalMonth(intervalMonth)
    }

    fun checkAmount(amount: BigDecimal, lockedValue: BigDecimal) : BigDecimal{
        val outputSize = (amount / lockedValue).toInt()
        val totalAmount = lockedValue * BigDecimal(outputSize)
        if (totalAmount > amount) {
            return (lockedValue.minus(BigDecimal((totalAmount % amount).toLong())))
        } else {
            return lockedValue
        }
    }

}
