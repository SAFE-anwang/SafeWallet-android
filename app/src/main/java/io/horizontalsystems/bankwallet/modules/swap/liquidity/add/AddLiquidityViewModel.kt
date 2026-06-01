package io.horizontalsystems.bankwallet.modules.swap.liquidity.add

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchServiceSendEvm
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchServiceSendEvm.AmountType
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.eip20approve.Eip20ApproveFragment
import io.horizontalsystems.bankwallet.modules.evmfee.GasDataError
import io.horizontalsystems.bankwallet.modules.multiswap.EvmBlockchainHelper
import io.horizontalsystems.bankwallet.modules.multiswap.TimerService
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionService
import io.horizontalsystems.bankwallet.modules.swap.ErrorShareService
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.liquidity.*
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityMainModule.AmountTypeItem
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityMainModule.ProviderViewItem
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityMainModule.SwapResultState
import io.horizontalsystems.bankwallet.modules.swap.liquidity.allowance.LiquidityAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.liquidity.allowance.LiquidityPendingAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.liquidity.allowance.SwapPendingAllowanceState
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.uniswapkit.Extensions
import io.horizontalsystems.uniswapkit.liquidity.PancakeSwapKit
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.UUID

class AddLiquidityViewModel(
    private val formatter: LiquidityViewItemHelper,
    val service: LiquidityMainService,
    private val switchService: AmountTypeSwitchServiceSendEvm,
    private val tokenAService: LiquidityTokenService,
    private val tokenBService: LiquidityTokenService,
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

    val logger = AppLogger("AddLiquidityViewModel")

    private val maxValidDecimals = 8
    private val disposable = CompositeDisposable()
    private val tradeDisposable = CompositeDisposable()

    private val dex: SwapMainModule.Dex
        get() = service.dex

    private val version: AddLiquidityModule.Version
        get() = AddLiquidityModule.getVersionFromProvider(dex.provider)

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

    private var exactType: SwapMainModule.ExactType = SwapMainModule.ExactType.ExactFrom
    private var balanceA: BigDecimal? = null
    private var balanceB: BigDecimal? = null
    private var availableBalance: String? = null
    private var availableBalanceB: String? = null
    private var amountTypeSelect = buildAmountTypeSelect()
    private var amountTypeSelectEnabled = switchService.toggleAvailable

    private var tokenAState = tokenAService.state
    private var tokenBState = tokenBService.state

    private val evmKit: EthereumKit by lazy {
        App.evmBlockchainManager.getEvmKitManager(dex.blockchainType).evmKitWrapper?.evmKit!!
    }
    private val uniswapKit by lazy { PancakeSwapKit.getInstance() }

    private var tradeService: LiquidityMainModule.ISwapTradeService = getTradeService(dex.provider)
    private var tradePriceExpiration: Float? = null
    private var amountA: BigDecimal? = null
    private var amountB: BigDecimal? = null
    private var allErrors: List<Throwable> = emptyList()
    private var allErrorsB: List<Throwable> = emptyList()
    private var error: String? = null
    private var hasNonZeroBalance: Boolean? = null
    private var hasNonZeroBalanceB: Boolean? = null

    private var buttons = SwapMainModule.SwapButtons2(
        SwapMainModule.SwapActionState.Hidden,
        SwapMainModule.SwapActionState.Hidden,
        SwapMainModule.SwapActionState.Hidden,
        SwapMainModule.SwapActionState.Hidden,
        SwapMainModule.SwapActionState.Hidden
    )
    private var refocusKey = UUID.randomUUID().leastSignificantBits

    // V3 specific
    private val feeTiers = AddLiquidityModule.FeeTier.all
    private var selectedFeeTier: AddLiquidityModule.FeeTier? = AddLiquidityModule.FeeTier.default
    private var priceRangeMin: String = ""
    private var priceRangeMax: String = ""
    private var currentPrice: String? = null
    private var hasAutoSetPriceRange = false
    private var isFetchingSwapData = false
    private var lastTokenA: Token? = null
    private var lastTokenB: Token? = null

    var sendStateObservable = SingleLiveEvent<SendEvmTransactionService.SendState>()
    var isInitTrade = true
    var isNoTrade = true

    var state by mutableStateOf(
        AddLiquidityModule.AddLiquidityState(
            version = version,
            dex = dex,
            providerViewItems = providerViewItems,
            tokenAState = tokenAState,
            tokenBState = tokenBState,
            availableBalance = availableBalance,
            availableBalanceB = availableBalanceB,
            amountTypeSelect = amountTypeSelect,
            amountTypeSelectEnabled = amountTypeSelectEnabled,
            error = error,
            buttons = buttons,
            hasNonZeroBalance = hasNonZeroBalance,
            refocusKey = refocusKey,
            feeTiers = feeTiers,
            selectedFeeTier = selectedFeeTier,
            priceRange = if (version == AddLiquidityModule.Version.V3)
                AddLiquidityModule.PriceRange(priceRangeMin, priceRangeMax, currentPrice)
            else null,
            showPriceRange = version == AddLiquidityModule.Version.V3,
        )
    )
        private set

    val approveData: Eip20ApproveFragment.Input?
        get() = getFromToken?.let { token ->
            allowanceServiceA.getSpenderAddress()?.let {
                Eip20ApproveFragment.Input(token, amountA ?: BigDecimal.ZERO, it)
            }
        }

    val approveDataB: Eip20ApproveFragment.Input?
        get() = getToToken()?.let { token ->
            allowanceServiceB.getSpenderAddress()?.let {
                Eip20ApproveFragment.Input(token, amountB ?: BigDecimal.ZERO, it)
            }
        }

    val getFromToken = tokenAService.token

    fun getToToken(): Token? = tokenBService.token

    init {
        tokenAService.stateFlow.collectWith(viewModelScope) {
            val tokenChanged = it.token != lastTokenA
            tokenAState = it
            lastTokenA = it.token
            syncUiState()
            if (tokenChanged && it.token != null) {
                tryResyncSwapData()
            }
        }
        tokenBService.stateFlow.collectWith(viewModelScope) {
            val tokenChanged = it.token != lastTokenB
            tokenBState = it
            lastTokenB = it.token
            syncUiState()
            if (tokenChanged && it.token != null) {
                tryResyncSwapData()
            }
        }

        service.providerUpdatedFlow.collectWith(viewModelScope) { provider ->
            allowanceServiceA.set(getSpenderAddress(provider))
            allowanceServiceB.set(getSpenderAddress(provider))
            tradeService = getTradeService(provider)
            tokenBService.setAmountEnabled(provider.supportsExactOut)
            syncUiState()
        }

        switchService.amountTypeObservable.subscribeIO {
            amountTypeSelect = buildAmountTypeSelect()
            syncUiState()
        }.let { disposable.add(it) }

        switchService.toggleAvailableObservable.subscribeIO {
            amountTypeSelectEnabled = it
            syncUiState()
        }.let { disposable.add(it) }

        allowanceServiceA.stateFlow.collectWith(viewModelScope) { syncSwapDataState() }
        allowanceServiceB.stateFlow.collectWith(viewModelScope) { syncSwapDataState() }

        pendingAllowanceServiceA.stateObservable.subscribeIO { syncSwapDataState() }
            .let { disposable.add(it) }
        pendingAllowanceServiceB.stateObservable.subscribeIO { syncSwapDataState() }
            .let { disposable.add(it) }

        allowanceServiceA.set(getSpenderAddress(dex.provider))
        allowanceServiceB.set(getSpenderAddress(dex.provider))
        tokenAService.token?.let {
            allowanceServiceA.set(it)
            pendingAllowanceServiceA.set(it)
        }
        tokenBService.token?.let {
            allowanceServiceB.set(it)
            pendingAllowanceServiceB.set(it)
        }

        tokenBService.setAmountEnabled(dex.provider.supportsExactOut)
        tokenAService.start()
        tokenBService.start()
        setBalance()
        subscribeToTradeService()
        timerService.start(20)
        allowanceServiceA.start()
        allowanceServiceB.start()
        syncButtonsState()

        Log.d("AddLiquidityViewModel", "Initialized with version: $version, provider: ${dex.provider.title}")
    }

    private fun getTradeService(provider: SwapMainModule.ISwapProvider): LiquidityMainModule.ISwapTradeService =
        when (provider) {
            LiquidityMainModule.PancakeV3LiquidityProvider ->
                LiquidityV3TradeService(uniswapKit, evmKit, EvmBlockchainHelper(dex.blockchainType).getRpcSourceHttp())
            LiquidityMainModule.UniswapV3LiquidityProvider ->
                LiquidityV3TradeService(uniswapKit, evmKit, EvmBlockchainHelper(dex.blockchainType).getRpcSourceHttp())
            LiquidityMainModule.PancakeLiquidityProvider ->
                LiquidityV2TradeService(uniswapKit, evmKit, EvmBlockchainHelper(dex.blockchainType).getRpcSourceHttp())
            LiquidityMainModule.Safe4LiquidityProvider ->
                LiquidityV2TradeService(uniswapKit, evmKit, EvmBlockchainHelper(dex.blockchainType).getRpcSourceHttp())
            else ->
                LiquidityV2TradeService(uniswapKit, evmKit, EvmBlockchainHelper(dex.blockchainType).getRpcSourceHttp())
        }

    private fun getSpenderAddress(provider: SwapMainModule.ISwapProvider) = when (provider) {
        LiquidityMainModule.PancakeLiquidityProvider -> uniswapKit.routerAddress(evmKit.chain)
        LiquidityMainModule.Safe4LiquidityProvider -> uniswapKit.routerAddress(evmKit.chain)
        LiquidityMainModule.PancakeV3LiquidityProvider -> uniswapKit.routerAddress(evmKit.chain, true)
        LiquidityMainModule.UniswapV3LiquidityProvider -> uniswapKit.routerAddress(evmKit.chain, true)
        else -> uniswapKit.routerAddress(evmKit.chain)
    }

    private fun syncUiState() {
        state = AddLiquidityModule.AddLiquidityState(
            version = version,
            dex = dex,
            providerViewItems = providerViewItems,
            tokenAState = wrapNativeForDisplay(tokenAState),
            tokenBState = wrapNativeForDisplay(tokenBState),
            availableBalance = availableBalance,
            availableBalanceB = availableBalanceB,
            amountTypeSelect = amountTypeSelect,
            amountTypeSelectEnabled = amountTypeSelectEnabled,
            error = error,
            buttons = buttons,
            hasNonZeroBalance = hasNonZeroBalance,
            refocusKey = refocusKey,
            feeTiers = feeTiers,
            selectedFeeTier = selectedFeeTier,
            priceRange = if (version == AddLiquidityModule.Version.V3)
                AddLiquidityModule.PriceRange(priceRangeMin, priceRangeMax, currentPrice)
            else null,
            showPriceRange = version == AddLiquidityModule.Version.V3,
        )
    }

    /**
     * 如果选中的是原生币(BSC的BNB、Ethereum的ETH), 自动显示为对应的包装代币(WBNB/WETH)
     */
    private fun wrapNativeForDisplay(coinState: LiquidityMainModule.SwapCoinCardViewState): LiquidityMainModule.SwapCoinCardViewState {
        val token = coinState.token ?: return coinState
        if (token.type !is TokenType.Native) return coinState

        val wrappedAddress = getWrappedNativeAddress(token.blockchainType)
        if (wrappedAddress.isEmpty()) return coinState

        val wrappedToken = App.evmBlockchainManager.getBaseToken(token.blockchainType, wrappedAddress)
        return wrappedToken?.let { coinState.copy(token = it) } ?: coinState
    }

    private fun getWrappedNativeAddress(blockchainType: BlockchainType): String = when (blockchainType) {
        BlockchainType.Ethereum -> "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"
        BlockchainType.BinanceSmartChain -> "0xbb4cdb9cbd36b01bd1cbaebf2de08d9173bc095c"
        BlockchainType.SafeFour -> "0x0000000000000000000000000000000000001101"
        BlockchainType.Polygon -> "0x0d500B1d8E8eF31E21C99d1Db9A6444d3ADf1270"
        BlockchainType.Optimism -> "0x4200000000000000000000000000000000000006"
        BlockchainType.ArbitrumOne -> "0x82aF49447D8a07e3bd95BD0d56f35241523fBab1"
        else -> ""
    }

    private fun subscribeToTradeService() {
        tradeService.stateFlow.collectWith(viewModelScope) { state ->
            syncSwapDataState()
        }
        viewModelScope.launch {
            timerService.stateFlow.collect {
                resyncSwapData()
                tradePriceExpiration = it.remaining?.toFloat()
                syncUiState()
            }
        }
    }

    private fun syncSwapDataState() {
        val errors = mutableListOf<Throwable>()
        val errorsB = mutableListOf<Throwable>()

        setLoading(tradeService.state)
        Log.d("AddLiquidityViewModel", "state=${tradeService.state}")

        when (val state = tradeService.state) {
            SwapResultState.Loading -> {}
            is SwapResultState.NotReady -> {
                isNoTrade = true
                isFetchingSwapData = false
            }
            is SwapResultState.Ready -> {
                isFetchingSwapData = false
                if (!isInitTrade) {
                    isInitTrade = false
                    return
                }
                isNoTrade = false
                when (val swapData = state.swapData) {
                    is LiquidityMainModule.SwapData.OneInchData -> {
                        amountB = swapData.data.amountTo
                        tokenBService.onChangeAmount(swapData.data.amountTo.toString(), true)
                    }
                    is LiquidityMainModule.SwapData.UniswapData -> {
                        if (exactType == SwapMainModule.ExactType.ExactFrom) {
                            amountB = swapData.data.amountOut
                            tokenBService.onChangeAmount(swapData.data.amountOut.toString(), true)
                        } else {
                            amountA = swapData.data.amountIn
                            tokenAService.onChangeAmount(swapData.data.amountIn.toString(), true)
                        }
                    }
                }
            }
        }

        // Auto-set V3 price range from pool current price (only once)
        if (version == AddLiquidityModule.Version.V3/* && !hasAutoSetPriceRange*/
            && tokenAService.token != null && tokenBService.token != null
        ) {
            val tradeService = tradeService as? ILiquidityTradeService
            // Only auto-set when swapData has been successfully fetched for the CURRENT pair
            // (not stale data from the singleton's previous swapDataV3 call)
            if (tradeService != null && tradeService.swapDataFetched) {
                val poolPrice = tradeService.currentPoolPriceHuman
                Log.d("AddLiquidityViewModel", "Auto-set check: poolPriceHuman=$poolPrice, hasAutoSetPriceRange=$hasAutoSetPriceRange")
                if (poolPrice != null && poolPrice > BigDecimal.ZERO) {
                    val priceScale = maxOf(tokenAService.token?.decimals ?: 18, tokenBService.token?.decimals ?: 18)
                    val minPrice = poolPrice.multiply(BigDecimal("0.8")).setScale(priceScale, RoundingMode.HALF_UP)
                    val maxPrice = poolPrice.multiply(BigDecimal("1.2")).setScale(priceScale, RoundingMode.HALF_UP)
                    priceRangeMin = minPrice.stripTrailingZeros().toPlainString()
                    priceRangeMax = maxPrice.stripTrailingZeros().toPlainString()
                    currentPrice = poolPrice.setScale(priceScale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
                    hasAutoSetPriceRange = true
                    Log.d("AddLiquidityViewModel", "Auto-set V3 price range: $priceRangeMin - $priceRangeMax (current: $currentPrice)")
                } else {
                    Log.w("AddLiquidityViewModel", "Auto-set V3 skipped: poolPriceHuman is null or zero. Raw poolPrice=${tradeService.currentPoolPrice}")
                }
            } else {
                Log.d("AddLiquidityViewModel", "Auto-set V3 waiting: swapData not yet fetched for current pair")
            }
        }

        // Check allowance for token A
        when (val stateA = allowanceServiceA.state) {
            LiquidityAllowanceService.State.Loading -> {}
            is LiquidityAllowanceService.State.Ready -> {
                amountA?.let { amt ->
                    if (amt > stateA.allowance.value) {
                        if (revokeRequired()) {
                            errors.add(LiquidityMainModule.SwapError.RevokeAllowanceRequired)
                        } else {
                            errors.add(LiquidityMainModule.SwapError.InsufficientAllowance)
                        }
                    }
                }
            }
            is LiquidityAllowanceService.State.NotReady -> errors.add(stateA.error)
            null -> {}
        }

        // Check allowance for token B
        when (val stateB = allowanceServiceB.state) {
            LiquidityAllowanceService.State.Loading -> {}
            is LiquidityAllowanceService.State.Ready -> {
                amountB?.let { amt ->
                    if (amt > stateB.allowance.value) {
                        if (revokeRequiredB()) {
                            errorsB.add(LiquidityMainModule.SwapError.RevokeAllowanceRequired)
                        } else {
                            errorsB.add(LiquidityMainModule.SwapError.InsufficientAllowance)
                        }
                    }
                }
            }
            is LiquidityAllowanceService.State.NotReady -> errorsB.add(stateB.error)
            null -> {}
        }

        // Check balance
        amountA?.let { amt ->
            if (balanceA == null || balanceA!! < amt) {
                errors.add(LiquidityMainModule.SwapError.InsufficientBalanceFrom)
            }
        }
        amountB?.let { amt ->
            if (balanceB == null || balanceB!! < amt) {
                errorsB.add(LiquidityMainModule.SwapError.InsufficientBalanceFrom)
            }
        }

        allErrors = errors
        allErrorsB = errorsB
        errorShareService.updateErrors(errors)

        val filtered = allErrors.filter { it !is GasDataError && it !is LiquidityMainModule.SwapError }
        error = filtered.firstOrNull()?.let { convert(it) }
        if (error == null) {
            val filteredB = allErrorsB.filter { it !is GasDataError && it !is LiquidityMainModule.SwapError }
            error = filteredB.firstOrNull()?.let { convert(it) }
        }

        syncUiState()
        syncButtonsState()
    }

    private fun syncButtonsState() {
        val revokeAction1 = getRevokeActionState()
        val approveAction1 = getApproveActionState(revokeAction1)
        val revokeAction2 = getRevokeActionState2()
        val approveAction2 = getApproveActionState2(revokeAction2)
        val proceedAction = getProceedActionState(revokeAction2)

        buttons = SwapMainModule.SwapButtons2(
            revokeAction1, revokeAction2, approveAction1, approveAction2, proceedAction
        )
        syncUiState()
    }

    private fun getProceedActionState(revokeAction: SwapMainModule.SwapActionState) = when {
        balanceA == null || balanceB == null ->
            SwapMainModule.SwapActionState.Disabled(Translator.getString(R.string.Swap_ErrorBalanceNotAvailable))
        revokeAction !is SwapMainModule.SwapActionState.Hidden ->
            SwapMainModule.SwapActionState.Hidden
        else -> when {
            allErrors.any { it == LiquidityMainModule.SwapError.InsufficientBalanceFrom } ->
                SwapMainModule.SwapActionState.Disabled(Translator.getString(R.string.Swap_ErrorInsufficientBalance))
            allErrorsB.any { it == LiquidityMainModule.SwapError.InsufficientBalanceFrom } ->
                SwapMainModule.SwapActionState.Disabled(Translator.getString(R.string.Swap_ErrorInsufficientBalance))
            pendingAllowanceServiceA.state == SwapPendingAllowanceState.Approving
                    || pendingAllowanceServiceB.state == SwapPendingAllowanceState.Approving ->
                SwapMainModule.SwapActionState.Disabled(Translator.getString(R.string.Liquidity_Add))
            else -> {
                if (allErrors.isEmpty() && allErrorsB.isEmpty()) {
                    if (allowanceServiceA.state !is LiquidityAllowanceService.State.Loading
                        && allowanceServiceB.state !is LiquidityAllowanceService.State.Loading
                    ) {
                        if (amountA != null && amountB != null) {
                            if (amountA!! > balanceA && amountB!! > balanceB) {
                                SwapMainModule.SwapActionState.Disabled(
                                    Translator.getString(R.string.Swap_ErrorInsufficientBalance)
                                )
                            } else {
                                SwapMainModule.SwapActionState.Enabled(
                                    Translator.getString(R.string.Liquidity_Add)
                                )
                            }
                        } else {
                            SwapMainModule.SwapActionState.Disabled(
                                Translator.getString(R.string.Liquidity_Add)
                            )
                        }
                    } else {
                        SwapMainModule.SwapActionState.Disabled(
                            Translator.getString(R.string.Liquidity_Add)
                        )
                    }
                } else {
                    SwapMainModule.SwapActionState.Disabled(
                        Translator.getString(R.string.Liquidity_Add)
                    )
                }
            }
        }
    }

    private fun getRevokeActionState() = when {
        allErrors.isNotEmpty() && allErrors.all { it == LiquidityMainModule.SwapError.RevokeAllowanceRequired } ->
            SwapMainModule.SwapActionState.Enabled(Translator.getString(R.string.Swap_Revoke))
        else -> SwapMainModule.SwapActionState.Hidden
    }

    private fun getRevokeActionState2() = when {
        allErrorsB.isNotEmpty() && allErrorsB.all { it == LiquidityMainModule.SwapError.RevokeAllowanceRequired } ->
            SwapMainModule.SwapActionState.Enabled(Translator.getString(R.string.Swap_Revoke))
        else -> SwapMainModule.SwapActionState.Hidden
    }

    private fun getApproveActionState(revokeAction: SwapMainModule.SwapActionState) = when {
        revokeAction !is SwapMainModule.SwapActionState.Hidden -> SwapMainModule.SwapActionState.Hidden
        pendingAllowanceServiceA.state == SwapPendingAllowanceState.Approving ->
            SwapMainModule.SwapActionState.Disabled(
                Translator.getString(R.string.Swap_Approving),
                loading = true
            )
        allErrors.any { it == LiquidityMainModule.SwapError.InsufficientBalanceFrom } ->
            SwapMainModule.SwapActionState.Hidden
        allErrors.any { it == LiquidityMainModule.SwapError.InsufficientAllowance } ->
            SwapMainModule.SwapActionState.Enabled(Translator.getString(R.string.Swap_Approve))
        pendingAllowanceServiceA.state == SwapPendingAllowanceState.Approved ->
            SwapMainModule.SwapActionState.Disabled(Translator.getString(R.string.Swap_Approve))
        else -> SwapMainModule.SwapActionState.Hidden
    }

    private fun getApproveActionState2(revokeAction: SwapMainModule.SwapActionState) = when {
        revokeAction !is SwapMainModule.SwapActionState.Hidden -> SwapMainModule.SwapActionState.Hidden
        pendingAllowanceServiceB.state == SwapPendingAllowanceState.Approving ->
            SwapMainModule.SwapActionState.Disabled(
                Translator.getString(R.string.Swap_Approving),
                loading = true
            )
        allErrorsB.any { it == LiquidityMainModule.SwapError.InsufficientBalanceFrom } ->
            SwapMainModule.SwapActionState.Hidden
        allErrorsB.any { it == LiquidityMainModule.SwapError.InsufficientAllowance } ->
            SwapMainModule.SwapActionState.Enabled(Translator.getString(R.string.Swap_Approve))
        pendingAllowanceServiceB.state == SwapPendingAllowanceState.Approved ->
            SwapMainModule.SwapActionState.Disabled(Translator.getString(R.string.Swap_Approve))
        else -> SwapMainModule.SwapActionState.Hidden
    }

    private fun tryResyncSwapData() {
        if (isFetchingSwapData) {
            Log.d("AddLiquidityViewModel", "tryResyncSwapData: already fetching, skipping")
            return
        }
        if (tokenAService.token != null && tokenBService.token != null) {
            Log.d("AddLiquidityViewModel", "tryResyncSwapData: both tokens ready, triggering fetch")
            resyncSwapData()
        }
    }

    private fun resyncSwapData() {
        if (isFetchingSwapData) {
            Log.d("AddLiquidityViewModel", "resyncSwapData: already fetching, skipping")
            return
        }
        isFetchingSwapData = true
        tradeService.fetchSwapData(
            tokenAService.token, tokenBService.token, amountA, amountB, exactType
        )
    }

    private fun setLoading(state: SwapResultState) {
        val loading = state == SwapResultState.Loading
        tokenAService.setLoading(loading)
        tokenBService.setLoading(loading)
    }

    private fun buildAmountTypeSelect() = Select(
        selected = switchService.amountType.item,
        options = listOf(AmountTypeItem.Coin, AmountTypeItem.Currency(currencyManager.baseCurrency.code))
    )

    private fun balance(coin: Token): BigDecimal? =
        (adapterManager.getAdapterForToken(coin) as? IBalanceAdapter)?.balanceData?.available

    private fun syncBalanceA(balance: BigDecimal?) {
        balanceA = balance
        val token = tokenAService.token
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
        balanceB = balance
        val token = tokenBService.token
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
        tokenAService.token?.let { syncBalanceA(balance(it)) }
        tokenBService.token?.let { syncBalanceB(balance(it)) }
    }

    // V3 price range actions
    fun onPriceRangeMinChange(value: String) {
        priceRangeMin = value
        syncUiState()
    }

    fun onPriceRangeMaxChange(value: String) {
        priceRangeMax = value
        syncUiState()
    }

    fun onSelectFeeTier(feeTier: AddLiquidityModule.FeeTier) {
        selectedFeeTier = feeTier
        syncUiState()
    }

    fun onToggleAmountType() {
        switchService.toggle()
    }

    fun onSelectFromCoin(token: Token) {
        tokenAService.onSelectCoin(token)
        syncBalanceA(balance(token))
        tokenAService.onChangeAmount(null, true)
        hasAutoSetPriceRange = false
        resyncSwapData()
        allowanceServiceA.set(token)
        pendingAllowanceServiceA.set(token)
    }

    fun onSelectToCoin(token: Token) {
        tokenBService.onSelectCoin(token)
        syncBalanceB(balance(token))
        tokenBService.onChangeAmount(null, true)
        hasAutoSetPriceRange = false
        resyncSwapData()
        allowanceServiceB.set(token)
        pendingAllowanceServiceB.set(token)
    }

    fun onFromAmountChange(amount: String?) {
        exactType = SwapMainModule.ExactType.ExactFrom
        val coinAmount = tokenAService.getCoinAmount(amount)
        if (amountsEqual(amountA, coinAmount)) return
        amountA = coinAmount
        tokenAService.onChangeAmount(amount)
        if (!isNoTrade && !isInitTrade) {
            tokenBService.onChangeAmount(null, true)
        }
        resyncSwapData()
        syncButtonsState()
        resetButtons()
    }

    fun onToAmountChange(amount: String?) {
        exactType = SwapMainModule.ExactType.ExactTo
        val coinAmount = tokenBService.getCoinAmount(amount)
        if (amountsEqual(amountB, coinAmount)) return
        amountB = coinAmount
        tokenBService.onChangeAmount(amount)
        if (!isNoTrade && !isInitTrade) {
            tokenAService.onChangeAmount(null, true)
        }
        resyncSwapData()
        syncButtonsState()
        resetButtons()
    }

    private fun resetButtons() {
        buttons = SwapMainModule.SwapButtons2(
            SwapMainModule.SwapActionState.Hidden,
            SwapMainModule.SwapActionState.Hidden,
            SwapMainModule.SwapActionState.Hidden,
            SwapMainModule.SwapActionState.Hidden,
            SwapMainModule.SwapActionState.Disabled(Translator.getString(R.string.Liquidity_Add))
        )
        syncUiState()
    }

    fun onTapSwitch() {
        val tokenA = tokenAService.token
        val tokenB = tokenBService.token
        tokenAService.setToken(tokenB)
        tokenBService.setToken(tokenA)
        resyncSwapData()
        setBalance()
        allowanceServiceA.set(tokenB)
        pendingAllowanceServiceA.set(tokenB)
        allowanceServiceB.set(tokenA)
        pendingAllowanceServiceB.set(tokenA)
    }

    fun setProvider(provider: SwapMainModule.ISwapProvider) {
        tradeService.stop()
        isFetchingSwapData = false
        service.setProvider(provider)
        Extensions.isSafeSwap = provider.id == "safe"
        hasAutoSetPriceRange = false
        subscribeToTradeService()
        timerService.stop()
        timerService.start(20)
        refocusKey = UUID.randomUUID().leastSignificantBits
        syncUiState()
    }

    fun onSetAmountInBalancePercent(percent: Int) {
        val coinDecimals = tokenAService.token?.decimals ?: maxValidDecimals
        val percentRatio = BigDecimal.valueOf(percent.toDouble() / 100)
        val coinAmount =
            balanceA?.multiply(percentRatio)?.setScale(coinDecimals, RoundingMode.FLOOR) ?: return
        val amount = tokenAService.getCoinAmount(coinAmount)
        onFromAmountChange(amount.toPlainString())
    }

    fun didApprove() {
        pendingAllowanceServiceA.syncAllowance()
    }

    fun didApproveB() {
        pendingAllowanceServiceB.syncAllowance()
    }

    fun getSendEvmData(): SendEvmData? {
        val tokenA = tokenAService.token ?: return null
        val tokenB = tokenBService.token ?: return null
        val tokenAAmount = amountA?.movePointRight(tokenA.decimals)?.toBigInteger() ?: return null
        val tokenBAmount = amountB?.movePointRight(tokenB.decimals)?.toBigInteger() ?: return null

        // For V3, validate price range
        val minPriceHuman: BigDecimal?
        val maxPriceHuman: BigDecimal?
        if (version == AddLiquidityModule.Version.V3) {
            if (priceRangeMin.isBlank() || priceRangeMax.isBlank()) {
                Log.w("AddLiquidityViewModel", "V3 requires price range")
                return null
            }
            if (selectedFeeTier == null) {
                Log.w("AddLiquidityViewModel", "V3 requires fee tier selection")
                return null
            }
            minPriceHuman = priceRangeMin.toBigDecimalOrNull()
            maxPriceHuman = priceRangeMax.toBigDecimalOrNull()
            if (minPriceHuman == null || maxPriceHuman == null) {
                Log.w("AddLiquidityViewModel", "V3 invalid price range values")
                return null
            }
        } else {
            minPriceHuman = null
            maxPriceHuman = null
        }

        // Convert human-readable prices to raw pool prices for SDK
        val liquidityTradeService = tradeService as? ILiquidityTradeService
        val minPrice: BigDecimal? = minPriceHuman?.let { liquidityTradeService?.toRawPrice(it) }
        val maxPrice: BigDecimal? = maxPriceHuman?.let { liquidityTradeService?.toRawPrice(it) }

        val uniswapTradeService = liquidityTradeService ?: return null
        val transactionData = try {
            uniswapTradeService.transactionData(
                tokenA, tokenB, evmKit.receiveAddress, tokenAAmount, tokenBAmount,
                minPrice, maxPrice
            )
        } catch (e: Exception) {
            Log.e("AddLiquidityViewModel", "getSendEvmData error=$e")
            return null
        }

        val swapInfo = SendEvmData.UniswapLiquidityInfo(
            estimatedIn = amountA ?: BigDecimal.ZERO,
            estimatedOut = amountB ?: BigDecimal.ZERO,
            slippage = null,
            deadline = null,
            recipientDomain = null,
            price = null,
            priceImpact = null
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

    private val AmountType.item: AmountTypeItem
        get() = when (this) {
            AmountType.Coin -> AmountTypeItem.Coin
            AmountType.Currency -> AmountTypeItem.Currency(currencyManager.baseCurrency.code)
        }

    private fun revokeRequired(): Boolean {
        val token = tokenAService.token ?: return false
        val allowance = approveData?.requiredAllowance ?: return false
        return allowance.compareTo(BigDecimal.ZERO) != 0 && isUsdt(token)
    }

    private fun revokeRequiredB(): Boolean {
        val token = tokenBService.token ?: return false
        val allowance = approveDataB?.requiredAllowance ?: return false
        return allowance.compareTo(BigDecimal.ZERO) != 0 && isUsdt(token)
    }

    private fun isUsdt(token: Token): Boolean =
        token.blockchainType is BlockchainType.Ethereum
                && token.type is TokenType.Eip20
                && (token.type as TokenType.Eip20).address.lowercase() == "0xdac17f958d2ee523a2206206994597c13d831ec7"

    private fun amountsEqual(amount1: BigDecimal?, amount2: BigDecimal?) = when {
        amount1 == null && amount2 == null -> true
        amount1 != null && amount2 != null && amount2.compareTo(amount1) == 0 -> true
        else -> false
    }

    private fun convert(error: Throwable): String = when (val convertedError = error.convertedError) {
        is JsonRpc.ResponseError.RpcError -> convertedError.error.message
        is EvmError.InsufficientLiquidity ->
            Translator.getString(R.string.EthereumTransaction_Error_InsufficientLiquidity)
        else -> convertedError.message ?: convertedError.javaClass.simpleName
    }

    override fun onCleared() {
        disposable.dispose()
        tradeDisposable.dispose()
        tradeService.stop()
        allowanceServiceA.onCleared()
        allowanceServiceB.onCleared()
        pendingAllowanceServiceA.onCleared()
        pendingAllowanceServiceB.onCleared()
        tokenAService.stop()
        tokenBService.stop()
    }
}
