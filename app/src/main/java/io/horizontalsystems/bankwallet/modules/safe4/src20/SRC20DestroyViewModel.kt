package io.horizontalsystems.bankwallet.modules.safe4.src20

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.safe4.CustomToken
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.core.toHexString
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicBoolean

class SRC20DestroyViewModel(
    val type: DeployType,
    val symbol: String,
    val address: String,
    val service: SRC20Service,
    val evmKitWrapper: EvmKitWrapper,
    val balance: BigDecimal
) : ViewModelUiState<DeployDestroyUIState>() {

    private val disposables = CompositeDisposable()
    private var additionalNumber: BigInteger = BigInteger.ZERO
    private var totalSupply: BigInteger = BigInteger.ZERO
//    private var balance: BigInteger = BigInteger.ZERO

    var showConfirmationDialog = false
    var sendResult by mutableStateOf<SendResult?>(null)
    val isUpdate = AtomicBoolean(false)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                totalSupply = service.totalSupply(type.type)
            } catch (e: Exception) {

            }
            emitState()
        }
    }

    override fun createState(): DeployDestroyUIState {
        return DeployDestroyUIState(
            NodeCovertFactory.formatSafe(totalSupply, code = symbol),
            App.numberFormatter.formatCoinFull(balance, code = symbol, 8),
            additionalNumber > BigInteger.ZERO,
            showConfirmationDialog
        )
    }

    fun setAdditionalNumber(number: String) {
        try {
            this.additionalNumber = number.toBigInteger()
            emitState()
        } catch (e: Exception) {

        }
    }


    fun showConfirm() {
        if (isUpdate.get())  return
        showConfirmationDialog = true
        emitState()
    }

    fun cancel() {
        showConfirmationDialog = false
        emitState()
    }

    fun update() {
        cancel()
        sendResult = SendResult.Sending
        viewModelScope.launch(Dispatchers.IO) {
            try {
                service.src20BurnableBurn(evmKitWrapper.signer!!.privateKey.toHexString(),
                    NodeCovertFactory.scaleConvert(additionalNumber.toBigDecimal())).blockingGet()
                sendResult = SendResult.Sent
            } catch (e: Exception) {
                e.printStackTrace()
                sendResult = SendResult.Failed(NodeCovertFactory.createCaution(e))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}