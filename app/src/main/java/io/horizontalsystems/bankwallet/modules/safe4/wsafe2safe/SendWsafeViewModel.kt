package io.horizontalsystems.bankwallet.modules.safe4.wsafe2safe

import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.address.AddressValidationException
import io.horizontalsystems.bankwallet.modules.safe4.SafeInfoManager
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmData
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.disposables.CompositeDisposable

class SendWsafeViewModel(
    val service: SendWsafeService,
    private val clearables: List<Clearable>
) : ViewModel() {

    var error by mutableStateOf<Throwable?>(null)
        private set

    private val disposable = CompositeDisposable()

    val proceedEnabledLiveData = MutableLiveData(false)
    val amountCautionLiveData = MutableLiveData<Caution?>(null)
    val proceedLiveEvent = SingleLiveEvent<SendEvmData>()

    val coin: PlatformCoin
        get() = service.coin

    init {
        service.stateObservable.subscribeIO { sync(it) }.let { disposable.add(it) }
        service.amountCautionObservable.subscribeIO { sync(it) }.let { disposable.add(it) }

        sync(service.state)
    }

    fun onClickProceed() {
        val safeInfoPO = SafeInfoManager.getSafeInfo()
        if (!safeInfoPO.eth.eth2safe) {
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
        }
    }

    private fun sync(state: SendWsafeService.State) {
        proceedEnabledLiveData.postValue(state is SendWsafeService.State.Ready)
    }

    private fun sync(amountCaution: SendWsafeService.AmountCaution) {
        var caution: Caution? = null
        if (amountCaution.error?.convertedError != null) {
            val text =
                amountCaution.error.localizedMessage ?: amountCaution.error.javaClass.simpleName
            caution = Caution(text, Caution.Type.Error)
        } else if (amountCaution.amountWarning == SendWsafeService.AmountWarning.CoinNeededForFee) {
            caution = Caution(
                Translator.getString(
                    R.string.EthereumTransaction_Warning_CoinNeededForFee, service.sendCoin.code
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

    fun onEnterAddress(wallet: Wallet, address: Address?) {
        val ethAddr = App.ethereumKitManager.evmKitWrapper?.evmKit?.receiveAddress?.hex?.let { Address(it, null) }
        // 设置钱包的ETH地址为交易的接收地址
        service.setRecipientAddress(ethAddr, address)
        // 验证地址是否Safe
        validateSafe(wallet, address)
    }

    fun validateSafe(wallet: Wallet, address: Address?) {
        val adapter = when (val adapter = App.adapterManager.getAdapterForWallet(wallet)) {
            is ISendSafeAdapter -> {
                adapter
            } else -> {
                throw Exception("No adapter found!")
            }
        }
        error = null
        address?.hex?.let {
            try {
                adapter.validate(it)
            } catch (e: Exception) {
                error = AddressValidationException.Invalid(e)
            }
        }
    }


}
