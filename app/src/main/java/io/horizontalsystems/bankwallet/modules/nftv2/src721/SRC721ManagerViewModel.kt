package io.horizontalsystems.bankwallet.modules.nftv2.src721

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.ethereumkit.core.toHexString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.web3j.protocol.Web3j
import java.math.BigInteger

class SRC721ManagerViewModel(
    private val web3j: Web3j,
    private val evmKitWrapper: EvmKitWrapper,
) : ViewModelUiState<SRC721ManagerUiState>() {

    sealed class Dialog {
        data class Burn(val info: SRC721ContractInfo) : Dialog()
        data class Remove(val info: SRC721ContractInfo) : Dialog()
    }

    private var items: List<SRC721ManagerItem> = emptyList()
    private var refreshing = false

    var activeDialog by mutableStateOf<Dialog?>(null)
    var burnTokenId by mutableStateOf("")
    var sendResult by mutableStateOf<SendResult?>(null)

    private val creator: String
        get() = evmKitWrapper.evmKit.receiveAddress.hex

    private val privateKey: String
        get() = evmKitWrapper.signer!!.privateKey.toHexString()

    init {
        refresh()
    }

    override fun createState(): SRC721ManagerUiState {
        return SRC721ManagerUiState(
            list = items,
            refreshing = refreshing
        )
    }

    fun refresh() {
        refreshing = true
        emitState()
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = SRC721Storage.list(creator).map { info ->
                try {
                    val service = SRC721Service(web3j, info.address)
                    SRC721ManagerItem(
                        info = info,
                        totalSupply = service.totalSupply().toString(),
                        remainSupply = service.remainSupply().toString(),
                    )
                } catch (e: Throwable) {
                    SRC721ManagerItem(info = info, loadFailed = true)
                }
            }
            withContext(Dispatchers.Main) {
                items = loaded
                refreshing = false
                emitState()
            }
        }
    }

    fun showBurnDialog(info: SRC721ContractInfo) {
        burnTokenId = ""
        activeDialog = Dialog.Burn(info)
    }

    fun showRemoveDialog(info: SRC721ContractInfo) {
        activeDialog = Dialog.Remove(info)
    }

    fun dismissDialog() {
        activeDialog = null
    }

    fun onEnterBurnTokenId(value: String) {
        burnTokenId = value.filter { it.isDigit() }
    }

    fun burn(info: SRC721ContractInfo) {
        val tokenId = burnTokenId.toBigIntegerOrNull() ?: return
        dismissDialog()
        sendResult = SendResult.Sending
        SRC721Service(web3j, info.address)
            .burn(privateKey, tokenId)
            .subscribeIO({
                sendResult = SendResult.Sent()
                refresh()
            }, { e ->
                sendResult = SendResult.Failed(NodeCovertFactory.createCaution(e))
            })
    }

    fun remove(info: SRC721ContractInfo) {
        dismissDialog()
        SRC721Storage.remove(info.address, creator)
        refresh()
    }
}
