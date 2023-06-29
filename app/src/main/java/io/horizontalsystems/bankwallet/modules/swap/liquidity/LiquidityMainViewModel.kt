package io.horizontalsystems.bankwallet.modules.swap.liquidity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.EvmError
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchService
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchService.AmountType
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.core.toHexString
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.evmfee.GasDataError
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionService
import io.horizontalsystems.bankwallet.modules.swap.ErrorShareService
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.TimerService
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapPendingAllowanceState
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityMainModule.AmountTypeItem
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityMainModule.ExactType
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityMainModule.PriceImpactLevel
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityMainModule.ProviderTradeData
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityMainModule.ProviderViewItem
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityMainModule.SwapData
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityMainModule.SwapError
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityMainModule.SwapResultState
import io.horizontalsystems.bankwallet.modules.swap.liquidity.allowance.LiquidityAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.liquidity.allowance.LiquidityPendingAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.Connect
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.Constants
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.TransactionContractSend
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.uniswapkit.Extensions
import io.horizontalsystems.uniswapkit.liquidity.PancakeSwapKit
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.utils.Convert
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.UUID


class LiquidityMainViewModel(
    private val formatter: LiquidityViewItemHelper,
    val service: LiquidityMainService,
    private val switchService: AmountTypeSwitchService,
    private val fromTokenService: LiquidityTokenService,
    private val toTokenService: LiquidityTokenService,
    private val allowanceServiceA: LiquidityAllowanceService,
    private val allowanceServiceB: LiquidityAllowanceService,
    private val pendingAllowanceServiceA: LiquidityPendingAllowanceService,
    private val pendingAllowanceServiceB: LiquidityPendingAllowanceService,
    private val errorShareService: ErrorShareService,
    private val timerService: TimerService,
    private val currencyManager: CurrencyManager,
    private val adapterManager: IAdapterManager,
    private val evmKitWrapper: EvmKitWrapper?
) : ViewModel() {

    val logger = AppLogger("LiquidityMainViewModel")

    private val maxValidDecimals = 8

    private val disposable = CompositeDisposable()
    private val tradeDisposable = CompositeDisposable()

    private val dex: SwapMainModule.Dex
        get() = service.dex

    val revokeEvmData: SendEvmData?
        get() = allowanceServiceA.revokeEvmData()

    val revokeEvmDataB: SendEvmData?
        get() = allowanceServiceB.revokeEvmData()

    private val providerViewItems: List<ProviderViewItem>
        get() = service.availableProviders.map {
            ProviderViewItem(
                provider = it,
                selected = it == dex.provider
            )
        }

    private var exactType: ExactType = ExactType.ExactFrom
    private var balanceFrom: BigDecimal? = null
    private var balanceFromB: BigDecimal? = null
    private var availableBalance: String? = null
    private var availableBalanceB: String? = null
    private var amountTypeSelect = buildAmountTypeSelect()

    private var amountTypeSelectEnabled = switchService.toggleAvailable

    private var tokenFromState = fromTokenService.state
    private var tokenToState = toTokenService.state

    private val evmKit: EthereumKit by lazy { App.evmBlockchainManager.getEvmKitManager(dex.blockchainType).evmKitWrapper?.evmKit!! }
    private val uniswapKit by lazy { PancakeSwapKit.getInstance(evmKit) }
//    private val uniswapV3Kit by lazy { UniswapV3Kit.getInstance(evmKit) }
    private var tradeService: LiquidityMainModule.ISwapTradeService = getTradeService(dex.provider)
    private var tradeView: LiquidityMainModule.TradeViewX? = null
    private var tradePriceExpiration: Float? = null

    private var amountFrom: BigDecimal? = null
    private var amountTo: BigDecimal? = null
    private var allErrors: List<Throwable> = emptyList()
    private var allErrorsB: List<Throwable> = emptyList()
    private var error: String? = null
    private var hasNonZeroBalance: Boolean? = null
    private var hasNonZeroBalanceB: Boolean? = null
    private var swapData: SwapData? = null
    private var buttons = SwapMainModule.SwapButtons2(
        SwapMainModule.SwapActionState.Hidden,
        SwapMainModule.SwapActionState.Hidden,
        SwapMainModule.SwapActionState.Hidden,
        SwapMainModule.SwapActionState.Hidden,
        SwapMainModule.SwapActionState.Hidden
    )
    private var refocusKey = UUID.randomUUID().leastSignificantBits

    var sendStateObservable = SingleLiveEvent<SendEvmTransactionService.SendState>()


    var swapState by mutableStateOf(
        LiquidityMainModule.SwapState(
            dex = dex,
            providerViewItems = providerViewItems,
            availableBalance = availableBalance,
            availableBalanceB = availableBalanceB,
            amountTypeSelect = amountTypeSelect,
            amountTypeSelectEnabled = amountTypeSelectEnabled,
            fromState = tokenFromState,
            toState = tokenToState,
            tradeView = tradeView,
            tradePriceExpiration = tradePriceExpiration,
            error = error,
            buttons = buttons,
            hasNonZeroBalance = hasNonZeroBalance,
            recipient = tradeService.recipient,
            slippage = tradeService.slippage,
            ttl = tradeService.ttl,
            refocusKey = refocusKey
        )
    )
        private set

    val approveData: SwapMainModule.ApproveData?
        get() = balanceFrom?.let { amount ->
            allowanceServiceA.approveData(dex, amount)
        }

    val approveDataB: SwapMainModule.ApproveData?
        get() = balanceFromB?.let { amount ->
            allowanceServiceB.approveData(dex, amount)
        }

    val proceedParams: SwapData?
        get() = swapData

    val getFromToken = fromTokenService.token

    init {
        fromTokenService.stateFlow.collectWith(viewModelScope) {
            tokenFromState = it
            syncUiState()
        }

        toTokenService.stateFlow.collectWith(viewModelScope) {
            tokenToState = it
            syncUiState()
        }

        service.providerUpdatedFlow.collectWith(viewModelScope) { provider ->
            allowanceServiceA.set(getSpenderAddress(provider))
            allowanceServiceB.set(getSpenderAddress(provider))
            tradeService = getTradeService(provider)
            toTokenService.setAmountEnabled(provider.supportsExactOut)
            syncUiState()
        }

        switchService.amountTypeObservable
            .subscribeIO {
                amountTypeSelect = buildAmountTypeSelect()
                syncUiState()
            }.let {
                disposable.add(it)
            }

        switchService.toggleAvailableObservable
            .subscribeIO {
                amountTypeSelectEnabled = it
                syncUiState()
            }.let {
                disposable.add(it)
            }

        allowanceServiceA.stateFlow
            .collectWith(viewModelScope) {
                syncSwapDataState()
            }
        allowanceServiceB.stateFlow
            .collectWith(viewModelScope) {
                syncSwapDataState()
            }

        pendingAllowanceServiceA.stateObservable
            .subscribeIO {
                syncSwapDataState()
            }.let {
                disposable.add(it)
            }
        pendingAllowanceServiceB.stateObservable
            .subscribeIO {
                syncSwapDataState()
            }.let {
                disposable.add(it)
            }


        allowanceServiceA.set(getSpenderAddress(dex.provider))
        allowanceServiceB.set(getSpenderAddress(dex.provider))
        fromTokenService.token?.let {
            allowanceServiceA.set(it)
            pendingAllowanceServiceA.set(it)
        }
        toTokenService.token?.let {
            allowanceServiceB.set(it)
            pendingAllowanceServiceB.set(it)
        }

        toTokenService.setAmountEnabled(dex.provider.supportsExactOut)
        fromTokenService.start()
        toTokenService.start()
        setBalance()
        subscribeToTradeService()
        timerService.start()
        allowanceServiceA.start()
        allowanceServiceB.start()
        syncButtonsState()
    }

    private fun getTradeService(provider: SwapMainModule.ISwapProvider): LiquidityMainModule.ISwapTradeService = when (provider) {
        LiquidityMainModule.PancakeLiquidityProvider -> LiquidityV2TradeService(uniswapKit)
        else -> LiquidityV2TradeService(uniswapKit)
    }

    private fun getSpenderAddress(provider: SwapMainModule.ISwapProvider) = when (provider) {
//        SwapMainModule.OneInchProvider -> oneIncKitHelper.smartContractAddress
        LiquidityMainModule.PancakeLiquidityProvider -> uniswapKit.routerAddress
        else -> uniswapKit.routerAddress
    }

    private fun syncUiState() {
        swapState = LiquidityMainModule.SwapState(
            dex = dex,
            providerViewItems = providerViewItems,
            availableBalance = availableBalance,
            availableBalanceB = availableBalanceB,
            amountTypeSelect = amountTypeSelect,
            amountTypeSelectEnabled = amountTypeSelectEnabled,
            fromState = tokenFromState,
            toState = tokenToState,
            tradeView = tradeView,
            tradePriceExpiration = tradePriceExpiration,
            error = error,
            buttons = buttons,
            hasNonZeroBalance = hasNonZeroBalance,
            recipient = tradeService.recipient,
            slippage = tradeService.slippage,
            ttl = tradeService.ttl,
            refocusKey = refocusKey
        )
    }

    private fun subscribeToTradeService() {
        tradeService.stateFlow.collectWith(viewModelScope) { state ->
            syncSwapDataState()
        }

        timerService.reSyncFlow.collectWith(viewModelScope) {
            resyncSwapData()
        }

        timerService.timeoutProgressFlow.collectWith(viewModelScope) {
            tradePriceExpiration = it
            syncUiState()
        }

    }

    private fun syncSwapDataState() {
        val errors = mutableListOf<Throwable>()
        val errorsB = mutableListOf<Throwable>()
        swapData = null
        setLoading(tradeService.state)

        when (val state = tradeService.state) {
            SwapResultState.Loading -> {
                tradeView = tradeView?.copy(expired = true)
            }

            is SwapResultState.NotReady -> {
                tradeView = null
                errors.addAll(state.errors)
                errorsB.addAll(state.errors)
            }

            is SwapResultState.Ready -> {
                swapData = state.swapData
                when (val swapData = state.swapData) {
                    is SwapData.OneInchData -> {
                        tradeView = oneInchTradeViewItem(swapData.data, fromTokenService.token, toTokenService.token)
                        amountTo = swapData.data.amountTo
                        toTokenService.onChangeAmount(swapData.data.amountTo.toString(), true)
                    }

                    is SwapData.UniswapData -> {
                        tradeView = uniswapTradeViewItem(swapData, fromTokenService.token, toTokenService.token)
                        if (exactType == ExactType.ExactFrom) {
                            amountTo = swapData.data.amountOut
                            toTokenService.onChangeAmount(swapData.data.amountOut.toString(), true)
                        } else {
                            amountFrom = swapData.data.amountIn
                            fromTokenService.onChangeAmount(swapData.data.amountIn.toString(), true)
                        }
                    }
                }
            }
        }

        when (val state = allowanceServiceA.state) {
            LiquidityAllowanceService.State.Loading -> {}

            is LiquidityAllowanceService.State.Ready -> {
                amountFrom?.let { amountFrom ->
                    if (amountFrom > state.allowance.value) {
                        if (revokeRequired()) {
                            errors.add(SwapError.RevokeAllowanceRequired)
                        } else {
                            errors.add(SwapError.InsufficientAllowance)
                        }
                    }
                }
            }

            is LiquidityAllowanceService.State.NotReady -> {
                errors.add(state.error)
            }

            null -> {}
        }

        when (val state = allowanceServiceB.state) {
            LiquidityAllowanceService.State.Loading -> {}

            is LiquidityAllowanceService.State.Ready -> {
                amountTo?.let { amountFrom ->
                    if (amountFrom > state.allowance.value) {
                        if (revokeRequired()) {
                            errors.add(SwapError.RevokeAllowanceRequired)
                        } else {
                            errors.add(SwapError.InsufficientAllowance)
                        }
                    }
                }
            }

            is LiquidityAllowanceService.State.NotReady -> {
                errors.add(state.error)
            }

            null -> {}
        }

        amountFrom?.let { amountFrom ->
            val balance = balanceFrom
            if (balance == null || balance < amountFrom) {
                errors.add(SwapError.InsufficientBalanceFrom)
            }
        }

        amountTo?.let { amountFrom ->
            val balance = balanceFromB
            if (balance == null || balance < amountFrom) {
                errorsB.add(SwapError.InsufficientBalanceFrom)
            }
        }

        if (pendingAllowanceServiceA.state.loading() || pendingAllowanceServiceB.state.loading()) {
            tradeView = tradeView?.copy(expired = true)
        }

        allErrors = errors
        allErrorsB = errorsB
        errorShareService.updateErrors(errors)

        val filtered = allErrors.filter { it !is GasDataError && it !is SwapError }
        error = filtered.firstOrNull()?.let { convert(it) }
        if (error == null) {
            val filtered = allErrorsB.filter { it !is GasDataError && it !is SwapError }
            error = filtered.firstOrNull()?.let { convert(it) }
        }

        syncUiState()
        syncButtonsState()
    }

    private fun setLoading(state: SwapResultState) {
        val loading = state == SwapResultState.Loading
        fromTokenService.setLoading(loading)
        toTokenService.setLoading(loading)
    }

    private fun resyncSwapData() {
        tradeService.fetchSwapData(fromTokenService.token, toTokenService.token, amountFrom, amountTo, exactType)
    }

    private fun syncButtonsState() {
        val revokeAction1 = getRevokeActionState()
        val approveAction1 = getApproveActionState(revokeAction1)
        val proceedAction = getProceedActionState(revokeAction1)

        val revokeAction2 = getRevokeActionState2()
        val approveAction2 = getApproveActionState2(revokeAction2)

        buttons = SwapMainModule.SwapButtons2(revokeAction1, revokeAction2, approveAction1, approveAction2, proceedAction)
        syncUiState()
    }

    private fun getProceedActionState(revokeAction: SwapMainModule.SwapActionState) = when {
        balanceFrom == null || balanceFromB == null -> {
            SwapMainModule.SwapActionState.Disabled(Translator.getString(R.string.Swap_ErrorBalanceNotAvailable))
        }

        revokeAction !is SwapMainModule.SwapActionState.Hidden -> {
            SwapMainModule.SwapActionState.Hidden
        }

        tradeService.state is SwapResultState.Ready -> {
            when {
                allErrors.any { it == SwapError.InsufficientBalanceFrom } -> {
                    SwapMainModule.SwapActionState.Disabled(Translator.getString(R.string.Swap_ErrorInsufficientBalance))
                }

                pendingAllowanceServiceA.state == SwapPendingAllowanceState.Approving
                 || pendingAllowanceServiceB.state == SwapPendingAllowanceState.Approving -> {
                    SwapMainModule.SwapActionState.Disabled(Translator.getString(R.string.Liquidity_Add))
                }

                else -> {
                    if (allErrors.isEmpty()) {
                        SwapMainModule.SwapActionState.Enabled(Translator.getString(R.string.Liquidity_Add))
                    } else {
                        SwapMainModule.SwapActionState.Disabled(Translator.getString(R.string.Liquidity_Add))
                    }
                }
            }
        }

        else -> {
            SwapMainModule.SwapActionState.Disabled(Translator.getString(R.string.Liquidity_Add))
        }
    }

    private fun getRevokeActionState() = when {
        pendingAllowanceServiceA.state == SwapPendingAllowanceState.Revoking -> {
            SwapMainModule.SwapActionState.Disabled(Translator.getString(R.string.Swap_Revoking))
        }

        allErrors.isNotEmpty() && allErrors.all { it == SwapError.RevokeAllowanceRequired } -> {
            SwapMainModule.SwapActionState.Enabled(Translator.getString(R.string.Swap_Revoke))
        }

        else -> {
            SwapMainModule.SwapActionState.Hidden
        }
    }

    private fun getRevokeActionState2() = when {
        pendingAllowanceServiceB.state == SwapPendingAllowanceState.Revoking -> {
            SwapMainModule.SwapActionState.Disabled(Translator.getString(R.string.Swap_Revoking))
        }

        allErrorsB.isNotEmpty() && allErrorsB.all { it == SwapError.RevokeAllowanceRequired } -> {
            SwapMainModule.SwapActionState.Enabled(Translator.getString(R.string.Swap_Revoke))
        }

        else -> {
            SwapMainModule.SwapActionState.Hidden
        }
    }

    private fun getApproveActionState(revokeAction: SwapMainModule.SwapActionState) = when {
        revokeAction !is SwapMainModule.SwapActionState.Hidden -> {
            SwapMainModule.SwapActionState.Hidden
        }

        pendingAllowanceServiceA.state == SwapPendingAllowanceState.Approving -> {
            SwapMainModule.SwapActionState.Disabled(Translator.getString(R.string.Swap_Approving), loading = true)
        }

        tradeService.state is SwapResultState.NotReady || allErrors.any { it == SwapError.InsufficientBalanceFrom } -> {
            SwapMainModule.SwapActionState.Hidden
        }

        allErrors.any { it == SwapError.InsufficientAllowance } -> {
            SwapMainModule.SwapActionState.Enabled(Translator.getString(R.string.Swap_Approve))
        }

        pendingAllowanceServiceA.state == SwapPendingAllowanceState.Approved -> {
            SwapMainModule.SwapActionState.Disabled(Translator.getString(R.string.Swap_Approve))
        }

        else -> {
            SwapMainModule.SwapActionState.Hidden
        }
    }

    private fun getApproveActionState2(revokeAction: SwapMainModule.SwapActionState) = when {
        revokeAction !is SwapMainModule.SwapActionState.Hidden -> {
            SwapMainModule.SwapActionState.Hidden
        }

        pendingAllowanceServiceB.state == SwapPendingAllowanceState.Approving -> {
            SwapMainModule.SwapActionState.Disabled(Translator.getString(R.string.Swap_Approving), loading = true)
        }
        tradeService.state is SwapResultState.NotReady || allErrorsB.any { it == SwapError.InsufficientBalanceFrom } -> {
            SwapMainModule.SwapActionState.Hidden
        }

        allErrorsB.any { it == SwapError.InsufficientAllowance } -> {
            SwapMainModule.SwapActionState.Enabled(Translator.getString(R.string.Swap_Approve))
        }

        pendingAllowanceServiceB.state == SwapPendingAllowanceState.Approved -> {
            SwapMainModule.SwapActionState.Disabled(Translator.getString(R.string.Swap_Approve))
        }

        else -> {
            SwapMainModule.SwapActionState.Hidden
        }
    }

    private fun buildAmountTypeSelect() = Select(
        selected = switchService.amountType.item,
        options = listOf(AmountTypeItem.Coin, AmountTypeItem.Currency(currencyManager.baseCurrency.code))
    )

    private fun balance(coin: Token): BigDecimal? =
        (adapterManager.getAdapterForToken(coin) as? IBalanceAdapter)?.balanceData?.available

    private fun syncBalance(balance: BigDecimal?) {
        balanceFrom = balance
        val token = fromTokenService.token
        val formattedBalance: String?
        val hasNonZeroBalance: Boolean?
        when {
            token == null -> {
                formattedBalance = Translator.getString(R.string.NotAvailable)
                hasNonZeroBalance = null
            }

            balance == null -> {
                formattedBalance = null
                hasNonZeroBalance = null
            }

            else -> {
                formattedBalance = formatter.coinAmount(balance, token.coin.code)
                hasNonZeroBalance = balance > BigDecimal.ZERO
            }
        }
        availableBalance = formattedBalance
        this.hasNonZeroBalance = hasNonZeroBalance
        syncUiState()
    }

    private fun syncBalanceB(balance: BigDecimal?) {
        balanceFromB = balance
        val token = toTokenService.token
        val formattedBalance: String?
        val hasNonZeroBalance: Boolean?
        when {
            token == null -> {
                formattedBalance = Translator.getString(R.string.NotAvailable)
                hasNonZeroBalance = null
            }

            balance == null -> {
                formattedBalance = null
                hasNonZeroBalance = null
            }

            else -> {
                formattedBalance = formatter.coinAmount(balance, token.coin.code)
                hasNonZeroBalance = balance > BigDecimal.ZERO
            }
        }
        availableBalanceB = formattedBalance
        this.hasNonZeroBalanceB = hasNonZeroBalance
        syncUiState()
    }

    private fun setBalance() {
        fromTokenService.token?.let {
            syncBalance(balance(it))
        }
        toTokenService.token?.let {
            syncBalanceB(balance(it))
        }
    }

    private fun oneInchTradeViewItem(params: LiquidityMainModule.OneInchSwapParameters, tokenFrom: Token?, tokenTo: Token?) = try {
        val sellPrice = params.amountTo.divide(params.amountFrom, params.tokenFrom.decimals, RoundingMode.HALF_UP).stripTrailingZeros()
        val buyPrice = params.amountFrom.divide(params.amountTo, params.tokenTo.decimals, RoundingMode.HALF_UP).stripTrailingZeros()
        val (primaryPrice, secondaryPrice) = formatter.prices(sellPrice, buyPrice, tokenFrom, tokenTo)
        LiquidityMainModule.TradeViewX(ProviderTradeData.OneInchTradeViewItem(primaryPrice, secondaryPrice))
    } catch (exception: ArithmeticException) {
        null
    }

    private fun uniswapTradeViewItem(swapData: SwapData.UniswapData, tokenFrom: Token?, tokenTo: Token?): LiquidityMainModule.TradeViewX {
        val (primaryPrice, secondaryPrice) = swapData.data.executionPrice?.let {
            val sellPrice = it
            val buyPrice = BigDecimal.ONE.divide(sellPrice, sellPrice.scale(), RoundingMode.HALF_EVEN)
            formatter.prices(sellPrice, buyPrice, tokenFrom, tokenTo)
        } ?: Pair(null, null)

        return LiquidityMainModule.TradeViewX(
            ProviderTradeData.UniswapTradeViewItem(
                primaryPrice = primaryPrice,
                secondaryPrice = secondaryPrice,
                priceImpact = formatter.priceImpactViewItem(swapData, PriceImpactLevel.Normal),
            )
        )
    }

    private val AmountType.item: AmountTypeItem
        get() = when (this) {
            AmountType.Coin -> AmountTypeItem.Coin
            AmountType.Currency -> AmountTypeItem.Currency(currencyManager.baseCurrency.code)
        }

    private fun revokeRequired(): Boolean {
        val tokenFrom = fromTokenService.token ?: return false
        val allowance = approveData?.allowance ?: return false

        return allowance.compareTo(BigDecimal.ZERO) != 0 && isUsdt(tokenFrom)
    }

    private fun isUsdt(token: Token): Boolean {
        val tokenType = token.type

        return token.blockchainType is BlockchainType.Ethereum
                && tokenType is TokenType.Eip20
                && tokenType.address.lowercase() == "0xdac17f958d2ee523a2206206994597c13d831ec7"
    }

    private fun amountsEqual(amount1: BigDecimal?, amount2: BigDecimal?): Boolean {
        return when {
            amount1 == null && amount2 == null -> true
            amount1 != null && amount2 != null && amount2.compareTo(amount1) == 0 -> true
            else -> false
        }
    }

    private fun convert(error: Throwable): String =
        when (val convertedError = error.convertedError) {
            is JsonRpc.ResponseError.RpcError -> {
                convertedError.error.message
            }

            is EvmError.InsufficientLiquidity -> {
                Translator.getString(R.string.EthereumTransaction_Error_InsufficientLiquidity)
            }

            else -> {
                convertedError.message ?: convertedError.javaClass.simpleName
            }
        }

    override fun onCleared() {
        disposable.dispose()
        tradeDisposable.dispose()
        tradeService.stop()
        allowanceServiceA.onCleared()
        allowanceServiceB.onCleared()
        pendingAllowanceServiceA.onCleared()
        pendingAllowanceServiceB.onCleared()
        fromTokenService.stop()
        toTokenService.stop()
    }

    fun onToggleAmountType() {
        switchService.toggle()
    }

    fun onSelectFromCoin(token: Token) {
        fromTokenService.onSelectCoin(token)
        syncBalance(balance(token))
        if (exactType == ExactType.ExactTo) {
            fromTokenService.onChangeAmount(null, true)
        }
        if (token == toTokenService.token) {
            toTokenService.setToken(null)
            toTokenService.onChangeAmount(null, true)
        }
        resyncSwapData()
        allowanceServiceA.set(token)
        pendingAllowanceServiceA.set(token)
    }

    fun onSelectToCoin(token: Token) {
        toTokenService.onSelectCoin(token)
        syncBalanceB(balance(token))
        if (exactType == ExactType.ExactFrom) {
            toTokenService.onChangeAmount(null, true)
        }
        if (token == fromTokenService.token) {
            fromTokenService.setToken(null)
            fromTokenService.onChangeAmount(null, true)
        }
        resyncSwapData()
        allowanceServiceB.set(token)
        pendingAllowanceServiceB.set(token)
    }

    fun onFromAmountChange(amount: String?) {
        exactType = ExactType.ExactFrom
        val coinAmount = fromTokenService.getCoinAmount(amount)
        if (amountsEqual(amountFrom, coinAmount)) return
        amountFrom = coinAmount
        amountTo = null
        fromTokenService.onChangeAmount(amount)
        toTokenService.onChangeAmount(null, true)
        resyncSwapData()
    }

    fun onToAmountChange(amount: String?) {
        exactType = ExactType.ExactTo
        val coinAmount = toTokenService.getCoinAmount(amount)
        if (amountsEqual(amountTo, coinAmount)) return
        amountTo = coinAmount
        amountFrom = null
        toTokenService.onChangeAmount(amount)
        fromTokenService.onChangeAmount(null, true)
        resyncSwapData()
    }

    fun onTapSwitch() {
        val fromToken = fromTokenService.token
        val toToken = toTokenService.token

        fromTokenService.setToken(toToken)
        toTokenService.setToken(fromToken)

        resyncSwapData()
        setBalance()
        allowanceServiceA.set(toToken)
        pendingAllowanceServiceA.set(toToken)
        allowanceServiceB.set(fromToken)
        pendingAllowanceServiceB.set(fromToken)
    }

    fun setProvider(provider: SwapMainModule.ISwapProvider) {
        tradeService.stop()
        service.setProvider(provider)
        Extensions.isSafeSwap = provider.id == "safe"
        subscribeToTradeService()

        timerService.stop()
        timerService.start()

        refocusKey = UUID.randomUUID().leastSignificantBits
        syncUiState()
    }

    fun onSetAmountInBalancePercent(percent: Int) {
        val coinDecimals = fromTokenService.token?.decimals ?: maxValidDecimals
        val percentRatio = BigDecimal.valueOf(percent.toDouble() / 100)
        val coinAmount = balanceFrom?.multiply(percentRatio)?.setScale(coinDecimals, RoundingMode.FLOOR) ?: return

        val amount = fromTokenService.getCoinAmount(coinAmount)
        onFromAmountChange(amount.toPlainString())
    }

    fun didApprove() {
        pendingAllowanceServiceA.syncAllowance()
    }

    fun didApproveB() {
        pendingAllowanceServiceB.syncAllowance()
    }

    fun getSendEvmData(swapData: SwapData.UniswapData): SendEvmData? {
        val uniswapTradeService = tradeService as? ILiquidityTradeService ?: return null
        val tradeOptions = uniswapTradeService.tradeOptions
        val transactionData = try {
            uniswapTradeService.transactionData(swapData.data)
        } catch (e: Exception) {
            return null
        }
        val gasPrice = Convert.toWei("5", Convert.Unit.GWEI).toBigInteger()
        val gasLimit = BigInteger("200000")


        val (primaryPrice, _) = swapData.data.executionPrice?.let {
            val sellPrice = it
            val buyPrice = BigDecimal.ONE.divide(sellPrice, sellPrice.scale(), RoundingMode.HALF_EVEN)
            formatter.prices(sellPrice, buyPrice, fromTokenService.token, toTokenService.token)
        } ?: Pair(null, null)

        val swapInfo = SendEvmData.UniswapLiquidityInfo(
            estimatedIn = amountFrom ?: BigDecimal.ZERO,
            estimatedOut = amountTo ?: BigDecimal.ZERO,
            slippage = formatter.slippage(tradeOptions.allowedSlippage),
            deadline = formatter.deadline(tradeOptions.ttl),
            recipientDomain = tradeOptions.recipient?.title,
            price = primaryPrice,
            priceImpact = formatter.priceImpactViewItem(swapData)
        )

        return SendEvmData(
            transactionData,
            SendEvmData.AdditionalInfo.Liquidity(swapInfo),
        )
    }

    fun onUpdateSwapSettings(recipient: Address?, slippage: BigDecimal?, ttl: Long?) {
        tradeService.updateSwapSettings(recipient, slippage, ttl)
        syncSwapDataState()
    }

    fun send(swapData: SwapData.UniswapData) {
        if (evmKitWrapper == null)  return
        val uniswapTradeService = tradeService as? ILiquidityTradeService ?: return
        val tradeOptions = uniswapTradeService.tradeOptions
        val transactionData = try {
            uniswapTradeService.transactionData(swapData.data)
        } catch (e: Exception) {
            return
        }

        sendStateObservable.value = SendEvmTransactionService.SendState.Sending
        logger.info("sending tx")
        GlobalScope.launch {
            try {
                val web3j: Web3j = Connect.connect()
                val nonce = web3j.ethGetTransactionCount(
                    evmKitWrapper.evmKit.receiveAddress.hex,
                    DefaultBlockParameterName.LATEST
                )
                    .send().transactionCount
                val hash = TransactionContractSend.send(
                    web3j, Credentials.create(evmKitWrapper.signer!!.privateKey.toString(16)),
                    Constants.DEX.PANCAKE_V2_ROUTER_ADDRESS,
                    transactionData.input.toHexString(),
                    BigInteger.ZERO, nonce,
                    Convert.toWei("10", Convert.Unit.GWEI).toBigInteger(),  // GAS PRICE : 5GWei
                    BigInteger("500000") // GAS LIMIT
                )
                Log.e(
                    "AddLiquidity",
                    "send result=${SendEvmTransactionService.SendState.Sent(hash.toByteArray())}"
                )
                withContext(Dispatchers.Main) {
                    sendStateObservable.value = SendEvmTransactionService.SendState.Sent(hash.toByteArray())
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    sendStateObservable.value = SendEvmTransactionService.SendState.Failed(e)
                }
            }
        }
        /*evmKitWrapper.sendSingle(
            transactionData,
            gasPrice,
            gasLimit,
            null
        )
            .subscribeIO({ fullTransaction ->
                sendStateObservable.value = SendEvmTransactionService.SendState.Sent(fullTransaction.transaction.hash)
                logger.info("success")
            }, { error ->
                Log.e("longwen", "send error=$error")
                sendStateObservable.value = SendEvmTransactionService.SendState.Failed(error)
                logger.warning("failed", error)
            })
            .let { disposable.add(it) }*/
    }

}
