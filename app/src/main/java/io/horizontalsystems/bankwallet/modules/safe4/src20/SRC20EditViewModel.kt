package io.horizontalsystems.bankwallet.modules.safe4.src20

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
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
import java.util.concurrent.atomic.AtomicBoolean

class SRC20EditViewModel(
    val type: DeployType,
    val service: SRC20Service,
    val evmKitWrapper: EvmKitWrapper
) : ViewModelUiState<DeployEditUIState>() {

    private val disposables = CompositeDisposable()
    private var whitePaperUrl: String? = null
    private var orgName: String? = null
    private var officialUrl: String? = null
    private var description: String? = null
    private var orgUpdate: Boolean = false
    private var officialUrlUpdate: Boolean = false
    private var whitePaperUrlUpdate: Boolean = false
    private var descriptionUpdate: Boolean = false

    var showConfirmationDialog = false
    var sendResult by mutableStateOf<SendResult?>(null)
    val isUpdate = AtomicBoolean(false)

    init {
        viewModelScope.launch {
            try {
                orgName = service.orgName(type.type)
                whitePaperUrl = service.whitePaperUrl(type.type)
                officialUrl = service.officialUrl(type.type)
                description = service .description(type.type)
                emitState()
            } catch (e: Exception) {

            }
        }
    }

    override fun createState(): DeployEditUIState {
        return DeployEditUIState(
            orgName,
            whitePaperUrl,
            officialUrl,
            description,
            orgUpdate || whitePaperUrlUpdate || officialUrlUpdate || descriptionUpdate,
            showConfirmationDialog
        )
    }

    fun setOrgName(orgName: String) {
        orgUpdate = this.orgName != orgName
        this.orgName = orgName
        emitState()
    }

    fun setOfficialUrl(officialUrl: String) {
        officialUrlUpdate = this.officialUrl != officialUrl
        this.officialUrl = officialUrl
        emitState()
    }

    fun setWhitePaperUrl(whitePaperUrl: String) {
        whitePaperUrlUpdate = this.whitePaperUrl != whitePaperUrl
        this.whitePaperUrl = whitePaperUrl
        emitState()
    }

    fun setDescription(description: String) {
        descriptionUpdate = this.description != description
        this.description = description
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
        cancel()
        sendResult = SendResult.Sending
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (whitePaperUrlUpdate && whitePaperUrl != null) {
                    service.setWhitePaperUrl(type.type, evmKitWrapper.signer!!.privateKey.toHexString(), whitePaperUrl!!)
                }
                if (orgUpdate && orgName != null) {
                    service.setWhitePaperUrl(type.type, evmKitWrapper.signer!!.privateKey.toHexString(), orgName!!)
                }
                if (officialUrlUpdate && officialUrl != null) {
                    service.setWhitePaperUrl(type.type, evmKitWrapper.signer!!.privateKey.toHexString(), officialUrl!!)
                }
                if (descriptionUpdate && description != null) {
                    service.setWhitePaperUrl(type.type, evmKitWrapper.signer!!.privateKey.toHexString(), description!!)
                }
                sendResult = SendResult.Sent
            } catch (e: Exception) {
                sendResult = SendResult.Failed(NodeCovertFactory.createCaution(e))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}