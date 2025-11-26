package io.horizontalsystems.bankwallet.modules.safe4.safe42usdt

import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.receive.ReceiveModule
import io.horizontalsystems.bankwallet.modules.safe4.SafeInfoManager
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.disposables.CompositeDisposable

class Safe42UsdtConvertSendViewModel(
    val service: Safe42UsdtConvertService,
    private val clearables: List<Clearable>
) : ViewModel() {

    var error by mutableStateOf<Throwable?>(null)
        private set

    private val disposable = CompositeDisposable()

    val proceedEnabledLiveData = MutableLiveData(false)
    val amountCautionLiveData = MutableLiveData<Caution?>(null)
    val proceedLiveEvent = SingleLiveEvent<SendEvmData>()

    val coin: Token
        get() = service.coin

    var isMatic: Boolean = false

    init {
        service.stateObservable.subscribeIO {
            sync(it) }.let { disposable.add(it) }
        service.amountCautionObservable.subscribeIO { sync(it) }.let { disposable.add(it) }

        sync(service.state)
    }

    fun onClickProceed() {
        val safeInfoPO = SafeInfoManager.getSafeInfo(true)
        if ((isMatic && !safeInfoPO.matic.safe2matic) || !safeInfoPO.eth.safe2eth) {
            Toast.makeText(App.instance, Translator.getString(R.string.Safe4_Disabled), Toast.LENGTH_SHORT).show()
            return
        }
        if (!service.isSendMinAmount(safeInfoPO)) {
            Toast.makeText(App.instance, Translator.getString(R.string.Safe4_USDT_Min_Fee, "0.1"), Toast.LENGTH_SHORT).show()
            return
        } else {
            (service.state as? Safe42UsdtConvertService.State.Ready)?.let { readyState ->
                proceedLiveEvent.postValue(readyState.sendData)
            }
        }
    }

    private fun sync(state: Safe42UsdtConvertService.State) {
        proceedEnabledLiveData.postValue(state is Safe42UsdtConvertService.State.Ready)
    }

    private fun sync(amountCaution: Safe42UsdtConvertService.AmountCaution) {
        var caution: Caution? = null
        if (amountCaution.error?.convertedError != null) {
            var text =
                amountCaution.error.localizedMessage ?: amountCaution.error.javaClass.simpleName
            if (text.startsWith("Read error:")){
                text = "获取手续费异常，请稍等"
            }
            caution = Caution(text, Caution.Type.Error)
        } else if (amountCaution.amountWarning == Safe42UsdtConvertService.AmountWarning.CoinNeededForFee) {
            caution = Caution(
                Translator.getString(
                    R.string.EthereumTransaction_Warning_CoinNeededForFee, service.sendCoin.coin.code
                ),
                Caution.Type.Warning
            )
        }

        amountCautionLiveData.postValue(caution)
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposable.clear()
        SafeInfoManager.clear()
    }

    fun onEnterAddress(safeWallet: Wallet, address: Address?) {
        val receiveAdapter = App.adapterManager.getReceiveAdapterForWallet(safeWallet) ?: throw ReceiveModule.NoReceiverAdapter()
        Log.i("safe4", "---receiveAdapter = ${receiveAdapter.receiveAddress}")
        val ethAddr = Address(receiveAdapter.receiveAddress, null)
        // 设置钱包的ETH地址为交易的接收地址
        service.setRecipientAddress(ethAddr, address)
    }


}
