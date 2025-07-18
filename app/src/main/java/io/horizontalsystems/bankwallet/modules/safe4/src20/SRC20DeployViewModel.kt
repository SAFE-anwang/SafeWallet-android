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
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.core.toHexString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class SRC20DeployViewModel(
    val service: SRC20Service,
    private val evmKitWrapper: EvmKitWrapper,
) : ViewModelUiState<DeployUiState>() {

    var type: DeployType = DeployType.SRC20
    var name: String = ""
    var symbol: String = ""
    var totalSupply: Int = 0

    private var isDeploying = AtomicBoolean(false)
    var showConfirmationDialog = false
    var sendResult by mutableStateOf<SendResult?>(null)


    override fun createState(): DeployUiState {
        return DeployUiState(
            type,
            getDeployDesc(),
            name.isNotEmpty() && symbol.isNotEmpty() && totalSupply > 0,
            showConfirmationDialog
        )
    }

    private fun getDeployDesc(): Int {
        return when(type) {
            DeployType.SRC20 -> R.string.SRC20_Deploy_Type_Normal_Desc
            DeployType.SRC20Burnable -> R.string.SRC20_Deploy_Type_Burnable_Desc
            DeployType.SRC20Mintable -> R.string.SRC20_Deploy_Type_Mintable_Desc
        }
    }

    fun onSelectType(index: Int) {
        type = DeployType.valueOf(index)
        emitState()
    }

    fun onEnterName(name: String) {
        this.name = name
        emitState()
    }

    fun onEnterSymbol(symbol: String) {
        this.symbol = symbol
        emitState()
    }

    fun onEnterSupply(supply: String) {
        try {
            this.totalSupply = supply.toInt()
        } catch (e: Exception) {

        }
        emitState()
    }

    fun showConfirm() {
        if (isDeploying.get())  return
        showConfirmationDialog = true
        emitState()
    }

    fun cancel() {
        showConfirmationDialog = false
        emitState()
    }

    fun deploy() {
        if (isDeploying.get())  return
        cancel()
        isDeploying.set(true)
        sendResult = SendResult.Sending
        val amount = NodeCovertFactory.scaleConvert(totalSupply)
        val deploy = when(type) {
            DeployType.SRC20 -> service.src20Deploy(
                evmKitWrapper.signer!!.privateKey.toHexString(),
                name,
                symbol,
                amount
            )
            DeployType.SRC20Mintable -> service.src20MintableDeploy(
                evmKitWrapper.signer!!.privateKey.toHexString(),
                name,
                symbol,
                amount
            )
            DeployType.SRC20Burnable -> service.src20BurnableDeploy(
                evmKitWrapper.signer!!.privateKey.toHexString(),
                name,
                symbol,
                amount
            )
        }.subscribeIO({
            isDeploying.set(false)
            sendResult = SendResult.Sent
        }, { e ->
            sendResult = SendResult.Failed(NodeCovertFactory.createCaution(e))
        })
    }

}