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
import java.util.concurrent.atomic.AtomicBoolean

class SRC20PromotionViewModel(
    val type: DeployType,
    val service: SRC20Service,
    val evmKitWrapper: EvmKitWrapper
) : ViewModelUiState<DeployPromotionUIState>() {

    private val disposables = CompositeDisposable()
    private var localImage: ByteArray? = null
    private var fee: String? = null

    var showConfirmationDialog = false
    var sendResult by mutableStateOf<SendResult?>(null)
    val isUpdate = AtomicBoolean(false)

    init {
        viewModelScope.launch {
            service.getLogoPayAmount(type.type)
                .subscribeIO {
                    fee = NodeCovertFactory.formatSafe(it)
                    emitState()
                }?.let {
                    disposables.add(it)
                }
        }
    }

    override fun createState(): DeployPromotionUIState {
        return DeployPromotionUIState(
            fee,
            localImage != null,
            showConfirmationDialog
        )
    }

    fun setImageUrl(image: ByteArray?) {
        this.localImage = image
        emitState()
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
        if (localImage == null) return
        cancel()
        sendResult = SendResult.Sending
        viewModelScope.launch(Dispatchers.IO) {
            try {
                localImage?.let {
                    service.setLogoPayAmount(type.type, evmKitWrapper.signer!!.privateKey.toHexString(), it).blockingGet()
                    sendResult = SendResult.Sent
                }
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