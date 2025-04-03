package io.horizontalsystems.bankwallet.modules.safe4.safe42wsafe

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
import io.horizontalsystems.bankwallet.modules.address.AddressValidationException
import io.horizontalsystems.bankwallet.modules.receive.ReceiveModule
import io.horizontalsystems.bankwallet.modules.safe4.SafeInfoManager
import io.horizontalsystems.bankwallet.modules.safe4.wsafe2safe.SendWsafeService
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.disposables.CompositeDisposable

class Safe4ConvertSendViewModel(
    val service: Safe4ConvertService,
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
        (service.state as? Safe4ConvertService.State.Ready)?.let { readyState ->
            proceedLiveEvent.postValue(readyState.sendData)
        }
        /*val safeInfoPO = SafeInfoManager.getSafeInfo()
        if ((isMatic && !safeInfoPO.matic.matic2safe) || !safeInfoPO.eth.eth2safe) {
            Toast.makeText(App.instance, Translator.getString(R.string.Safe4_Disabled), Toast.LENGTH_SHORT).show()
            return
        }
        if (!service.isSendMinAmount(safeInfoPO)) {
            Toast.makeText(App.instance, Translator.getString(R.string.Safe4_Min_Fee, safeInfoPO.minamount), Toast.LENGTH_SHORT).show()
            return
        } else {
            (service.state as? SendWsafeService.State.Ready)?.let { readyState ->
                proceedLiveEvent.postValue(readyState.sendData)
            }
        }*/
    }

    private fun sync(state: Safe4ConvertService.State) {
        proceedEnabledLiveData.postValue(state is Safe4ConvertService.State.Ready)
    }

    private fun sync(amountCaution: Safe4ConvertService.AmountCaution) {
        var caution: Caution? = null
        if (amountCaution.error?.convertedError != null) {
            var text =
                amountCaution.error.localizedMessage ?: amountCaution.error.javaClass.simpleName
            if (text.startsWith("Read error:")){
                text = "获取手续费异常，请稍等"
            }
            caution = Caution(text, Caution.Type.Error)
        } else if (amountCaution.amountWarning == Safe4ConvertService.AmountWarning.CoinNeededForFee) {
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
