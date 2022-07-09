package io.horizontalsystems.bankwallet.modules.send.submodules.hodler

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.hodler.LockTimeInterval
import java.math.BigDecimal

object SendHodlerModule {
    interface IView {
        fun showLockTimeIntervalSelector(items: List<LockTimeIntervalViewItem>)
        fun setSelectedLockTimeInterval(timeInterval: LockTimeInterval?)
        fun setLockedValue(value: String)
        fun setStartMonth(value: String)
        fun setIntervalMonth(value: String)
    }

    interface IViewDelegate {
        fun onViewDidLoad()
        fun onClickLockTimeInterval()
        fun onSelectLockTimeInterval(position: Int)
        fun onTextChangeLockedValue(value: String)
        fun onTextChangeStartMonth(value: String)
        fun onTextChangeIntervalMonth(value: String)
    }

    interface IInteractor {
        fun getLockTimeIntervals(): Array<LockTimeInterval>
    }

    interface IHodlerModule {
        fun pluginData(): Map<Byte, IPluginData>
        fun setAmount(amount: BigDecimal)
    }

    interface IHodlerModuleDelegate {
        fun onUpdateLockTimeInterval(timeInterval: LockTimeInterval?)
        fun onUpdateLineLock(lockedValue: String?, startMonth: String?, intervalMonth: String?)
    }

    data class LockTimeIntervalViewItem(val lockTimeInterval: LockTimeInterval?, val selected: Boolean)

    class Factory(private val sendHandler: SendModule.ISendHandler, private val hodlerModuleDelegate: IHodlerModuleDelegate) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val view = SendHodlerView()
            val interactor = SendHodlerInteractor()
            val presenter = SendHodlerPresenter(view, interactor)

            presenter.moduleDelegate = hodlerModuleDelegate
            sendHandler.hodlerModule = presenter

            return presenter as T
        }
    }
}
