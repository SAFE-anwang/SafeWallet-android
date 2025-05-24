package io.horizontalsystems.bankwallet.modules.safe4.swap

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastCbrt
import androidx.lifecycle.ViewModel
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.adapters.Eip20Adapter
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.wsafe2safe.SendWsafeService
import io.horizontalsystems.bankwallet.modules.safe4.wsafe2safe.SendWsafeService.AmountCaution
import io.horizontalsystems.bankwallet.modules.safe4.wsafe2safe.SendWsafeService.AmountError
import io.horizontalsystems.bankwallet.modules.safe4.wsafe2safe.SendWsafeService.AmountWarning
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.wsafekit.Web3jUtils
import java.math.BigDecimal
import java.math.BigInteger

class Safe4SwapViewModel(
    private val token1: Token,
    private val token2: Token,
    val adapter: ISendEthereumAdapter,
    private val adapterManager: IAdapterManager,
    private val connectivityManager: ConnectivityManager,
): ViewModel() {
    companion object {
        val safe4SwapContractAddress = "0x0000000000000000000000000000000000001101"
    }

    private var fromToken = token1
    private var toToken = token2

    private var balanceFrom: BigDecimal = BigDecimal.ZERO
    private var balanceTo: BigDecimal = BigDecimal.ZERO
    private var availableBalance1: String = "0"
    private var availableBalance2: String = "0"
    private var evmAmount: BigInteger? = null
    private var inputAmount: String? = null

    private var amountCaution: AmountCaution? = null
    private var caution: Caution? = null

    var swapState by mutableStateOf(
        Safe4SwapModule.Safe4SwapUiState(
            canSend = false,
            fromToken = fromToken,
            toToken = toToken,
            balance1 = availableBalance1,
            balance2 = availableBalance2,
            caution = caution
        )
    )

    init {
        balance()
        if (token2.type == TokenType.Eip20(safe4SwapContractAddress)) {
            (App.adapterManager.getAdapterForToken(token2) as Eip20Adapter).refresh()
        }
    }

    private fun balance() {
        balanceFrom = (adapterManager.getAdapterForToken(token1) as? IBalanceAdapter)?.balanceData?.available ?: BigDecimal.ZERO
        availableBalance1 = App.numberFormatter.formatCoinFull(balanceFrom, token1.coin.code, 8)
        balanceTo = (adapterManager.getAdapterForToken(token2) as? IBalanceAdapter)?.balanceData?.available ?: BigDecimal.ZERO
        availableBalance2 = App.numberFormatter.formatCoinFull(balanceTo, token2.coin.code, 8)
        syncUiState()
    }

    fun onTapSwitch() {
        if (fromToken == token1) {
            fromToken = token2
            toToken = token1
        } else {
            fromToken = token1
            toToken = token2
        }
        syncUiState()
    }

    fun onChangeAmount(inputAmount: String) {
        this.inputAmount = inputAmount
        val amount = if (inputAmount.isEmpty())  {
            BigDecimal.ZERO
        } else {
            inputAmount.toBigDecimal()
        }
        val balance = if (fromToken == token1) balanceFrom else balanceTo
        if (amount > BigDecimal.ZERO) {
            var amountWarning: AmountWarning? = null
            try {
                if (amount == balance) {
                    amountWarning = AmountWarning.CoinNeededForFee
                }
                evmAmount = validEvmAmount(amount)
                amountCaution = AmountCaution(null, amountWarning)
            } catch (error: Throwable) {
                evmAmount = null
                amountCaution = AmountCaution(error, null)
            }
        } else {
            evmAmount = null
            amountCaution = null
        }
        sync(amountCaution)
        syncUiState()
    }

    @Throws
    private fun validEvmAmount(amount: BigDecimal): BigInteger {
        val evmAmount = try {
            amount.movePointRight(fromToken.decimals).toBigInteger()
        } catch (error: Throwable) {
            throw AmountError.InvalidDecimal
        }
        val available = if (fromToken == token1) balanceFrom else balanceTo
        if (amount > available) {
            throw AmountError.InsufficientBalance
        }
        return evmAmount
    }


    private fun sync(amountCaution: AmountCaution?) {
        if (amountCaution?.error?.convertedError != null) {
            var text =
                amountCaution.error.localizedMessage ?: amountCaution.error.javaClass.simpleName
            if (text.startsWith("Read error:")){
                text = "获取手续费异常，请稍等"
            }
            caution = Caution(text, Caution.Type.Error)
        } else if (amountCaution?.amountWarning == AmountWarning.CoinNeededForFee) {
            caution = Caution(
                Translator.getString(
                    R.string.EthereumTransaction_Warning_CoinNeededForFee, fromToken.coin.code
                ),
                Caution.Type.Warning
            )
        }

    }

    private fun syncUiState() {
        swapState = Safe4SwapModule.Safe4SwapUiState(
            canSend = evmAmount!= null && evmAmount!! > BigInteger.ZERO && caution == null,
            fromToken = fromToken,
            toToken = toToken,
            balance1 = getBalance(fromToken),
            balance2 = getBalance(toToken),
            caution = caution,
            inputAmount = inputAmount
        )
    }

    private fun getBalance(token: Token): String {
        return when(token) {
            token1 -> availableBalance1
            token2 -> availableBalance2
            else -> ""
        }
    }

    fun hasConnection(): Boolean {
        return connectivityManager.isConnected
    }

    fun getSendEvmData(): SendEvmData {
        val input = if (fromToken == token1)
            Web3jUtils.getSafe4SwapSrcTransactionInput()
        else
            Web3jUtils.getSrcSwapSafe4TransactionInput(evmAmount)
        val additionalInfo = SendEvmData.AdditionalInfo.Send(SendEvmData.SendInfo())
        val transactionData = TransactionData(to = Address( safe4SwapContractAddress), value = evmAmount!!,
            input.hexStringToByteArray(),
            safe4Swap = if (fromToken == token1) 1 else 2
        )
        return SendEvmData(transactionData, additionalInfo)
    }
}