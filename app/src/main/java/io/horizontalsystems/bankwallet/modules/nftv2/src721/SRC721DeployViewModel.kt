package io.horizontalsystems.bankwallet.modules.nftv2.src721

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.ethereumkit.core.toHexString
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicBoolean

class SRC721DeployViewModel(
    private val service: SRC721Service,
    private val evmKitWrapper: EvmKitWrapper,
) : ViewModelUiState<SRC721DeployUiState>() {

    var type: SRC721DeployType = SRC721DeployType.SRC721
    var name: String = ""
    var symbol: String = ""
    var baseURI: String = ""
    var maxSupply: BigInteger = BigInteger.ZERO
    var mintPrice: BigDecimal = BigDecimal.ZERO

    private var isDeploying = AtomicBoolean(false)
    var showConfirmationDialog = false
    var sendResult by mutableStateOf<SendResult?>(null)

    override fun createState(): SRC721DeployUiState {
        return SRC721DeployUiState(
            type,
            getDeployDesc(),
            name.isNotEmpty() && symbol.isNotEmpty() && maxSupply > BigInteger.ZERO,
            showConfirmationDialog
        )
    }

    private fun getDeployDesc(): Int {
        return when (type) {
            SRC721DeployType.SRC721 -> R.string.SRC721_Deploy_Type_Normal_Desc
            SRC721DeployType.SRC721Burnable -> R.string.SRC721_Deploy_Type_Burnable_Desc
        }
    }

    fun onSelectType(index: Int) {
        type = SRC721DeployType.valueOf(index)
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

    fun onEnterBaseURI(baseURI: String) {
        this.baseURI = baseURI
        emitState()
    }

    fun onEnterMaxSupply(supply: String) {
        try {
            this.maxSupply = supply.toBigInteger()
        } catch (e: Exception) {
            this.maxSupply = BigInteger.ZERO
        }
        emitState()
    }

    fun onEnterMintPrice(price: String) {
        try {
            this.mintPrice = price.toBigDecimal()
        } catch (e: Exception) {
            this.mintPrice = BigDecimal.ZERO
        }
        emitState()
    }

    fun showConfirm() {
        if (isDeploying.get()) return
        showConfirmationDialog = true
        emitState()
    }

    fun cancel() {
        showConfirmationDialog = false
        emitState()
    }

    fun deploy() {
        if (isDeploying.get()) return
        cancel()
        isDeploying.set(true)
        sendResult = SendResult.Sending

        val privateKey = evmKitWrapper.signer!!.privateKey.toHexString()
        val mintPriceWei = NodeCovertFactory.scaleConvert(mintPrice)

        val deploy = when (type) {
            SRC721DeployType.SRC721 -> service.deploy(
                privateKey, name, symbol, baseURI, maxSupply, mintPriceWei
            )
            SRC721DeployType.SRC721Burnable -> service.deployBurnable(
                privateKey, name, symbol, baseURI, maxSupply, mintPriceWei
            )
        }.subscribeIO({ result ->
            isDeploying.set(false)
            val contractAddress = result.firstOrNull()
            if (!contractAddress.isNullOrEmpty()) {
                SRC721Storage.save(
                    SRC721ContractInfo(
                        address = contractAddress,
                        name = name,
                        symbol = symbol,
                        burnable = type == SRC721DeployType.SRC721Burnable,
                        creator = evmKitWrapper.evmKit.receiveAddress.hex
                    )
                )
            }
            sendResult = SendResult.Sent()
        }, { e ->
            isDeploying.set(false)
            sendResult = SendResult.Failed(NodeCovertFactory.createCaution(e))
        })
    }
}
