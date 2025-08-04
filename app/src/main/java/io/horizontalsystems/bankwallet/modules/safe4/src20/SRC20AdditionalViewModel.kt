package io.horizontalsystems.bankwallet.modules.safe4.src20

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
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
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicBoolean

class SRC20AdditionalViewModel(
    val type: DeployType,
    val symbol: String,
    val address: String,
    val service: SRC20Service,
    val evmKitWrapper: EvmKitWrapper
) : ViewModelUiState<DeployAdditionalUIState>() {

    private val disposables = CompositeDisposable()
    private var additionalNumber: Int = 0
    private var totalSupply: BigInteger = BigInteger.ZERO
    private var balance: BigInteger = BigInteger.ZERO

    var showConfirmationDialog = false
    var sendResult by mutableStateOf<SendResult?>(null)
    val isUpdate = AtomicBoolean(false)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                totalSupply = service.totalSupply(type.type)
                balance = service.balance(evmKitWrapper.evmKit.receiveAddress.hex, type.type)
            } catch (e: Exception) {

            }
            emitState()
        }
    }

    override fun createState(): DeployAdditionalUIState {
        return DeployAdditionalUIState(
            NodeCovertFactory.formatSafe(totalSupply, code = symbol),
            NodeCovertFactory.formatSafe(balance, code = symbol),
            additionalNumber > 0,
            showConfirmationDialog
        )
    }

    fun setAdditionalNumber(number: String) {
        try {
            this.additionalNumber = number.toInt()
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
                val amount = NodeCovertFactory.scaleConvert(additionalNumber)
                if (type == DeployType.SRC20Mintable) {
                    service.src20MintableMint(
                        evmKitWrapper.signer!!.privateKey.toHexString(),
                        address,
                        amount
                    ).blockingGet()
                } else {
                    service.src20BurnableMint(
                        evmKitWrapper.signer!!.privateKey.toHexString(),
                        address,
                        amount
                    ).blockingGet()
                }
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