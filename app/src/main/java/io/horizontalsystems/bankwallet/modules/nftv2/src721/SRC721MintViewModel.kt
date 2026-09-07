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
import java.util.concurrent.atomic.AtomicBoolean

data class SRC721MintUiState(
    val address: String = "",
    val amount: String = "",
    val isOwner: Boolean = false,
    val mintPrice: BigInteger? = null,
    val allowedAmount: BigInteger? = null,
    val cost: BigInteger? = null,
    val proceedEnabled: Boolean = false,
)

/**
 * SRC721 铸造页：
 * - 合约 owner：走 adminMint（免费，可直接铸给任意地址）
 * - 非 owner：走 mint（需支付 amount * mintPrice，且地址需在 allowList 有额度）
 */
class SRC721MintViewModel(
    private val contract: SRC721ContractInfo,
    private val web3j: Web3j,
    private val evmKitWrapper: EvmKitWrapper,
) : ViewModelUiState<SRC721MintUiState>() {

    private var address = ""
    private var amount: BigInteger = BigInteger.ZERO
    private var isOwner = false
    private var mintPrice: BigInteger? = null
    private var allowedAmount: BigInteger? = null

    private val isMinting = AtomicBoolean(false)
    var sendResult by mutableStateOf<SendResult?>(null)

    private val receiveAddress: String
        get() = evmKitWrapper.evmKit.receiveAddress.hex

    private val privateKey: String
        get() = evmKitWrapper.signer!!.privateKey.toHexString()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val service = SRC721Service(web3j, contract.address)
            try {
                val owner = service.owner()
                isOwner = owner.equals(receiveAddress, ignoreCase = true)
            } catch (e: Throwable) {
            }
            try {
                mintPrice = service.mintPrice()
            } catch (e: Throwable) {
            }
            if (!isOwner) {
                try {
                    allowedAmount = service.amountAllowToMint(receiveAddress)
                } catch (e: Throwable) {
                }
            }
            withContext(Dispatchers.Main) {
                emitState()
            }
        }
    }

    override fun createState(): SRC721MintUiState {
        return SRC721MintUiState(
            address = address,
            amount = if (amount > BigInteger.ZERO) amount.toString() else "",
            isOwner = isOwner,
            mintPrice = mintPrice,
            allowedAmount = allowedAmount,
            cost = cost(),
            proceedEnabled = proceedEnabled()
        )
    }

    private fun cost(): BigInteger? {
        if (isOwner || amount <= BigInteger.ZERO) return null
        return mintPrice?.multiply(amount)
    }

    private fun proceedEnabled(): Boolean {
        if (address.isBlank() || amount <= BigInteger.ZERO) return false
        // 非 owner 时，数量不能超过铸造额度
        if (!isOwner) {
            val allowed = allowedAmount ?: return false
            if (amount > allowed) return false
        }
        return true
    }

    fun onAddressChange(value: String) {
        address = value.trim()
        emitState()
    }

    fun onAmountChange(value: String) {
        try {
            amount = value.toBigInteger()
        } catch (e: Exception) {
            amount = BigInteger.ZERO
        }
        emitState()
    }

    fun mint() {
        if (isMinting.get() || !proceedEnabled()) return
        isMinting.set(true)
        sendResult = SendResult.Sending

        val service = SRC721Service(web3j, contract.address)
        val single = if (isOwner) {
            service.adminMint(privateKey, address, amount)
        } else {
            val value = mintPrice?.multiply(amount) ?: BigInteger.ZERO
            service.mint(privateKey, value, address, amount)
        }

        single.subscribeIO({
            isMinting.set(false)
            sendResult = SendResult.Sent()
        }, { e ->
            isMinting.set(false)
            sendResult = SendResult.Failed(NodeCovertFactory.createCaution(e))
        })
    }
}
