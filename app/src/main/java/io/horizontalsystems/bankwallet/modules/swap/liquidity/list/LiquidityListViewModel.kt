package io.horizontalsystems.bankwallet.modules.swap.liquidity.list

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IWalletStorage
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountValidator
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityPair
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.BalanceOf
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.Connect
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.Constants
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.MethodID
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.Name
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.PairAddressNonce
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.PermitData
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.Token
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.TokenAmount
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.TotalSupply
import io.horizontalsystems.bankwallet.modules.swap.liquidity.util.TransactionContractSend
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenEntity
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Bytes32
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.abi.datatypes.generated.Uint8
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Sign
import org.web3j.crypto.StructuredDataEncoder
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode


class LiquidityListViewModel(
    private val accountManager: IAccountManager,
    private val storage: IWalletStorage,
    private val adapterManager: IAdapterManager,
    private val marketKit: MarketKitWrapper,
    private val liquidityViewItemFactory: LiquidityViewItemFactory
) : ViewModel() {

    val logger = AppLogger("LiquidityListViewModel")

    private val disposable = CompositeDisposable()

    private var liquidityItemsBSC = mutableListOf<LiquidityListModule.LiquidityItem>()
    private var liquidityItemsEth = mutableListOf<LiquidityListModule.LiquidityItem>()
    private var liquidityViewItemsBSC = listOf<LiquidityViewItem>()
    private var liquidityViewItemsEth = listOf<LiquidityViewItem>()
    private var viewState: ViewState = ViewState.Loading
    private var isRefreshing = false

    private val amountValidator = AmountValidator()
    var amountCaution: HSCaution? = null
    private var inputRemoveAmount: BigDecimal? = null
    var tempItem: LiquidityViewItem? = null
    var tempIndex: Int? = null

    private var removePercent = 25
    private var requestCount = 1
    private val Max_Request_Count = 5
    var uiState by mutableStateOf(
        LiquidityUiState(
            liquidityViewItems = liquidityViewItemsBSC,
            viewState = viewState,
            true,
                removePercent
        )
    )
        private set

    val tabs = LiquidityListModule.Tab.values()
    var selectedTab by mutableStateOf(LiquidityListModule.Tab.BSC)
        private set

    private var web3jBsc = Connect.connect(false)
    private var web3jEth = Connect.connect(true)

    val removeErrorMessage = SingleLiveEvent<String?>()
    val removeSuccessMessage = SingleLiveEvent<String>()

    private var amount = ""
    private var secondaryInfo: String = ""
    private var isEstimated = false
    private var isLoading = false
    private var amountEnabled = true
    private var validDecimals: Int = 0
    val state: SwapMainModule.SwapAmountInputState
        get() = SwapMainModule.SwapAmountInputState(
                    amount = amount,
                    secondaryInfo = secondaryInfo,
                    primaryPrefix = null,
                    validDecimals = validDecimals,
                    amountEnabled = amountEnabled,
                    dimAmount = isLoading && isEstimated,
                )

    private var getLiquidityJob: Job? = null

    private fun getWethAddress(chain: BlockchainType): String {
        val wethAddressHex = when (chain) {
            BlockchainType.Ethereum -> "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"
            BlockchainType.Optimism -> "0x4200000000000000000000000000000000000006"
            BlockchainType.BinanceSmartChain -> "0xbb4cdb9cbd36b01bd1cbaebf2de08d9173bc095c"
            BlockchainType.Polygon -> "0x0d500B1d8E8eF31E21C99d1Db9A6444d3ADf1270"
            BlockchainType.Avalanche -> "0xB31f66AA3C1e785363F0875A1B74E27b85FD66c7"
            BlockchainType.ArbitrumOne -> "0x82aF49447D8a07e3bd95BD0d56f35241523fBab1"
            else -> ""
        }
        return wethAddressHex
    }

    init {
        getAllLiquidity(isSelectBSC())
    }

    fun onEnterAmount(amount: BigDecimal?, availableBalance: BigDecimal) {
        amountCaution = amountValidator.validate(
                coinAmount = amount,
                coinCode = "",
                availableBalance = availableBalance,
                leaveSomeBalanceForFee = false
        )
        inputRemoveAmount = amount
        emitState()
    }

    fun reset() {
        amountCaution = null
        inputRemoveAmount = null
        emitState()
    }

    fun setRemovePercent(percent: Int) {
        removePercent = percent
        emitState()
    }

    fun refresh() {
        getAllLiquidity(isSelectBSC())
    }

    private fun getTokenEntity(uids: List<String>, type: String):List<TokenEntity> {
        return marketKit.getTokenEntity(uids, type)
    }

    fun onSelect(tab: LiquidityListModule.Tab) {
        getLiquidityJob?.cancel()
        selectedTab = tab
//        web3j = Connect.connect(!isSelectBSC())
//        liquidityViewItemsBSC = listOf()
//        refresh()
        emitState()
    }

    private fun getAllLiquidity(isBSC: Boolean) {
        viewState = ViewState.Loading
        val activeWallets = accountManager.activeAccount?.let {
            storage.wallets(it).filter {
                    it.token.blockchainType is BlockchainType.BinanceSmartChain
                            && it.token.coin.code != "Cake-LP"
            }
        } ?: listOf()
        val activeETHWallets = accountManager.activeAccount?.let {
            storage.wallets(it).filter {
                    it.token.blockchainType is BlockchainType.Ethereum
                            && it.token.coin.code != "UNI-V2"
            }
        } ?: listOf()
        if (activeWallets.isEmpty() && activeETHWallets.isEmpty()) return
        val uids = activeWallets.map { it.coin.uid }
        val uidsEth = activeETHWallets.map { it.coin.uid }
        val tokenEntityList = getTokenEntity(uids,"binance-smart-chain")
        val ethTokenEntityList = getTokenEntity(uidsEth,"ethereum")
        liquidityItemsBSC.clear()
        liquidityItemsEth.clear()
        /*getLiquidityJob = */viewModelScope.launch(Dispatchers.IO) {
            isRefreshing = true
            emitState()
            var requestError = false
            val list = mutableListOf<LiquidityViewItem>()
            val listEth = mutableListOf<LiquidityViewItem>()
            getAllPair(activeWallets).forEach { pair ->
                requestCount = 1
                requestError = false
                val tokenEntityA = tokenEntityList.find { it.coinUid == pair.first.coin.uid }?.reference ?: getWethAddress(pair.first.token.blockchain.type)
                val tokenEntityB = tokenEntityList.find { it.coinUid == pair.second.coin.uid }?.reference ?: getWethAddress(pair.second.token.blockchain.type)
                if (tokenEntityA != null && tokenEntityB != null) {
                    try {
                        getLiquidity(pair.first, tokenEntityA, pair.second, tokenEntityB, false)?.let {
                            liquidityItemsBSC.add(it)
                            list.add(liquidityViewItemFactory.viewItem(it))
                        }
                    } catch (e: Exception) {
                        if (requestCount == Max_Request_Count) {
                            viewState = ViewState.Error(e)
                        } else {
                            requestCount ++
                        }
                        requestError = true
                    }
                }
            }
            getAllPair(activeETHWallets).forEach { pair ->
                requestCount = 1
                requestError = false
                val tokenEntityA = ethTokenEntityList.find { it.coinUid == pair.first.coin.uid }?.reference ?: getWethAddress(pair.first.token.blockchain.type)
                val tokenEntityB = ethTokenEntityList.find { it.coinUid == pair.second.coin.uid }?.reference ?: getWethAddress(pair.second.token.blockchain.type)
                if (tokenEntityA != null && tokenEntityB != null) {
                    try {
                        getLiquidity(pair.first, tokenEntityA, pair.second, tokenEntityB, true)?.let {
                            liquidityItemsEth.add(it)
                            listEth.add(liquidityViewItemFactory.viewItem(it))
                        }
                    } catch (e: Exception) {
                        if (requestCount == Max_Request_Count) {
                            viewState = ViewState.Error(e)
                        } else {
                            requestCount ++
                        }
                        requestError = true
                    }
                }
            }
            if (requestError && requestCount == Max_Request_Count) {
                refresh()
            }
            viewState = ViewState.Success
            liquidityViewItemsBSC = list.map { it }
            liquidityViewItemsEth = listEth.map { it }
            isRefreshing = false
            emitState()
         }
    }



    private fun getAllPair(activeWallets: List<Wallet>): List<Pair<Wallet, Wallet>> {
        val pairs = mutableListOf<Pair<Wallet, Wallet>>()
        for (i in activeWallets.indices) {
            for (j in i + 1 until activeWallets.size) {
                pairs.add(Pair(activeWallets[i], activeWallets[j]))
            }
        }
        return pairs
    }

    private fun getLiquidity(walletA: Wallet, tokenAAddress: String, walletB: Wallet, tokenBAddress: String, isEth: Boolean): LiquidityListModule.LiquidityItem? {
        val adapterA = adapterManager.getReceiveAdapterForWallet(walletA) ?: return null
        val tokenA = Token(
            tokenAAddress,
            walletA.coin.name,
            walletA.coin.code,
            walletA.decimal
        )
        val tokenB = Token(
            tokenBAddress,
            walletB.coin.name,
            walletB.coin.code,
            walletB.decimal
        )
        val web3j = if (isEth) web3jEth else web3jBsc
        val pair = LiquidityPair.getPairReservesForPancakeSwap(web3j, tokenA, tokenB, isEth) ?: return null

        val pairAddress = pair.get(0) as String
        val token0 = pair.get(1) as Token
        val token1 = pair.get(2) as Token
        val isChange = token0.address != tokenAAddress

        val r0: BigInteger = pair[3] as BigInteger
        val r1: BigInteger = pair[4] as BigInteger

        // 查询 流动性代币的数量
        val poolTokenTotalSupply = TotalSupply.getTotalSupply(web3j, pairAddress)
        // 查询 当前用户拥有的流动性代币数量
        val balanceOfAccount = BalanceOf.balanceOf(web3j, pairAddress, adapterA.receiveAddress)
        Log.d("Pool Token TotalSupply = {}", "$poolTokenTotalSupply")
        Log.d("BalanceOf {} = {}", "${adapterA.receiveAddress}, ${balanceOfAccount}")
        // 计算用户在池子中的流动性占比
        var shareRate = BigDecimal(balanceOfAccount).divide(
            BigDecimal(poolTokenTotalSupply), 18, RoundingMode.DOWN
        )

        Log.d(
            "用户流动性占比:{}",
            shareRate.toString() + "%"
        )
        val pooledR0Amount = TokenAmount.toBigDecimal(
            token0,
            r0, token0.decimals
        ).multiply(shareRate)
        val pooledR1Amount = TokenAmount.toBigDecimal(
            token1,
            r1, token1.decimals
        ).multiply(shareRate)

        // 没有添加流动性
        if (balanceOfAccount.equals(BigInteger.ZERO))   return null
        return LiquidityListModule.LiquidityItem(
                if (isChange) walletB else walletA,
                if (isChange) walletA else walletB,
                tokenAAddress,
                tokenBAddress,
                pooledR0Amount,
                pooledR1Amount,
                balanceOfAccount,
                shareRate.multiply(BigDecimal("100")),
                poolTokenTotalSupply
            )
    }

    private fun emitState() {
        val newUiState = LiquidityUiState(
            liquidityViewItems = if (isSelectBSC()) liquidityViewItemsBSC else liquidityViewItemsEth,
            viewState = viewState,
            isRefreshing = isRefreshing,
            removePercent,
            amountCaution = amountCaution
        )

        viewModelScope.launch {
            uiState = newUiState
        }
    }

    private fun removeItem(removeIndex: Int) {
        val list = mutableListOf<LiquidityViewItem>()
        liquidityViewItemsBSC.forEachIndexed{ index, liquidityViewItem ->
            if (removeIndex != index) {
                list.add(liquidityViewItem)
            }
        }
        liquidityViewItemsBSC = list
        emitState()
    }

    fun removeLiquidity(index: Int, walletA: Wallet, tokenAAddress: String, walletB: Wallet, tokenBAddress: String) {
        if (amountCaution != null) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val evmKitWrapper =
                    App.evmBlockchainManager.getEvmKitManager(walletA.token.blockchainType).evmKitWrapper
                        ?: return@launch
                val tokenA = Token(
                    tokenAAddress,
                    walletA.coin.name,
                    walletA.coin.code,
                    walletA.decimal
                )
                val tokenB = Token(
                    tokenBAddress,
                    walletB.coin.name,
                    walletB.coin.code,
                    walletB.decimal
                )
                val web3j = if (isSelectBSC()) web3jBsc else web3jEth
                val pair =
                    LiquidityPair.getPairReservesForPancakeSwap(web3j, tokenA, tokenB, !isSelectBSC()) ?: return@launch
                val pairAddress = pair.get(0) as String
                val token00 = pair.get(1) as Token
                val token11 = pair.get(2) as Token
                //val isChange = token00.address != tokenAAddress
                val isChange = false
                val token0 = if (isChange) token11 else token00
                val token1 = if (isChange) token00 else token11

                val r0: BigInteger = pair[3] as BigInteger
                val r1: BigInteger = pair[4] as BigInteger

                // 查询 流动性代币的数量
                val poolTokenTotalSupply = TotalSupply.getTotalSupply(web3j, pairAddress)
                // 查询 当前用户拥有的流动性代币数量
                val balanceOfAccount = BalanceOf.balanceOf(
                        web3j,
                    pairAddress,
                    evmKitWrapper!!.evmKit.receiveAddress.hex
                )
                Log.d("Pool Token TotalSupply = {}", "$poolTokenTotalSupply")
                Log.d(
                    "BalanceOf {} = {}",
                    "${evmKitWrapper!!.evmKit.receiveAddress.hex}, ${balanceOfAccount}}"
                )

                // 计算用户在池子中的流动性占比
                val shareRate = BigDecimal(balanceOfAccount).divide(
                    BigDecimal(poolTokenTotalSupply), 18, RoundingMode.DOWN
                )
                Log.i(
                    "用户流动性占比:{}",
                    shareRate.multiply(BigDecimal("100")).toString() + "%"
                )
                val pooledR0Amount =
                    TokenAmount.toBigDecimal(token0, r0, token0.decimals).multiply(shareRate)
                val pooledR1Amount =
                    TokenAmount.toBigDecimal(token1, r1, token1.decimals).multiply(shareRate)
                Log.i("用户在池子中抵押:{} ({})", "$pooledR0Amount, ${token0.symbol}")
                Log.i("用户在池子中抵押:{} ({})", "$pooledR1Amount, ${token1.symbol}")

                // 用户期望移除抵押 的代币 的百分比;
                val removePercent = (removePercent / 100f).toString()
                val removeLiquidityAmount =
                    BigDecimal(balanceOfAccount).multiply(BigDecimal(removePercent)).toBigInteger()
                // 将会得到的 token0 和 token1 的数量
                val removeToken0Amount = pooledR0Amount.multiply(BigDecimal(removePercent))
                val removeToken1Amount = pooledR1Amount.multiply(BigDecimal(removePercent))
                Log.i(
                    "Receive {} Amount : {}",
                    "${token0.symbol}, $removeToken0Amount"
                )
                Log.i(
                    "Receive {} Amount : {}",
                    "${token1.symbol}, $removeToken1Amount"
                )
                // 通过用户设置的滑点,计算最低移除数量.
                val removeMinToken0Amount =
                    removeToken0Amount.multiply(BigDecimal.ONE.subtract(BigDecimal(Constants.slippage)))
                val removeMinToken1Amount = removeToken1Amount.multiply(
                    BigDecimal.ONE.subtract(
                        BigDecimal(
                            Constants.slippage
                        )
                    )
                )


                // 通过合约获取流动性代币的名称 , PancakeSwap 的流动性代币名称都是 "Pancake LPs"
                val name = Name.name(web3j, pairAddress)
                val nonces = PairAddressNonce.nonce(
                        web3j,
                    pairAddress,
                    evmKitWrapper!!.evmKit.receiveAddress.hex
                )
                val deadline = Constants.getDeadLine()
                val routerAddress = if (isSelectBSC()) {
                    Constants.DEX.PANCAKE_V2_ROUTER_ADDRESS
                } else {
                    Constants.DEX.UNISWAP_V2_ROUTER_ADDRESS
                }
                val chainId = if (isSelectBSC()) {
                    56
                } else {
                    1
                }
                val json = buildSignJson(
                    name,
                    chainId,
                    pairAddress,
                    evmKitWrapper!!.evmKit.receiveAddress.hex,
                    routerAddress,
                    removeLiquidityAmount,
                    nonces,
                    deadline
                )

                val ecKeyPair = ECKeyPair.create(evmKitWrapper!!.signer!!.privateKey)
                val structuredDataEncoder = StructuredDataEncoder(json)
                val signatureData =
                    Sign.signMessage(structuredDataEncoder.hashStructuredData(), ecKeyPair, false)
                val inputParameters: MutableList<Type<*>> = mutableListOf()
                /*
            function removeLiquidityWithPermit(
                address tokenA,
                address tokenB,
                uint liquidity,
                uint amountAMin,
                uint amountBMin,
                address to,
                uint deadline,
                bool approveMax,
                uint8 v, bytes32 r, bytes32 s
            ) external returns (uint amountA, uint amountB);
         */
                inputParameters.add(Address(token0.address))
                inputParameters.add(Address(token1.address))
                inputParameters.add(Uint256(removeLiquidityAmount))
                inputParameters.add(
                    Uint256(
                        TokenAmount.toRawBigInteger(
                            token0,
                            removeMinToken0Amount
                        )
                    )
                )
                inputParameters.add(
                    Uint256(
                        TokenAmount.toRawBigInteger(
                            token1,
                            removeMinToken1Amount
                        )
                    )
                )
                inputParameters.add(Address(evmKitWrapper.evmKit.receiveAddress.hex))
                inputParameters.add(Uint256(deadline))
                inputParameters.add(Bool(false))
                inputParameters.add(Uint8(Numeric.toBigInt(signatureData.v)))
                inputParameters.add(Bytes32(signatureData.r))
                inputParameters.add(Bytes32(signatureData.s))
                val encode = FunctionEncoder.encode(
                    MethodID.generate("removeLiquidityWithPermit(address,address,uint256,uint256,uint256,address,uint256,bool,uint8,bytes32,bytes32)"),
                    inputParameters
                )
                Log.d("removeLiquidityWithPermit encode result:{}", encode)

                // 获取 nonce
                val nonce =
                    web3j.ethGetTransactionCount(
                        evmKitWrapper.evmKit.receiveAddress.hex,
                        DefaultBlockParameterName.LATEST
                    )
                        .send().transactionCount
                val gasPrice: BigInteger = web3j.ethGasPrice()
                        .send()
                        .getGasPrice()
                val hash = TransactionContractSend.send(
                        web3j, Credentials.create(evmKitWrapper.signer!!.privateKey.toString(16)),
                        routerAddress,
                    encode,
                    BigInteger.ZERO, nonce,
                    gasPrice,  // GAS PRICE : 5GWei
                    BigInteger("500000") // GAS LIMIT
                )
                // 0xab43576d55e54c3c51ecff56b030cd83945ec7ee0892539953a8ee467570a73d
                // 0xab43576d55e54c3c51ecff56b030cd83945ec7ee0892539953a8ee467570a73d
                Log.i("Execute Router.removeLiquidityWithPermit Hash ", "= $hash")
                withContext(Dispatchers.Main) {
                    if (hash != null) {
                        removeItem(index)
                        removeSuccessMessage.value = if (App.languageManager.currentLanguage == "zh") {
                            "移除成功"
                        } else {
                            "Remove Success"
                        }
                    } else {
                        removeErrorMessage.value = if (App.languageManager.currentLanguage == "zh") {
                            "移除失败"
                        } else {
                            "Remove Fail"
                        }
                    }
                }
             //   refresh()
            } catch (e: Throwable) {
                Log.e("removeLiquidity", "error=$e")
                withContext(Dispatchers.Main) {
                    removeErrorMessage.value = e.message
                }
            }
        }
    }


    private fun buildSignJson(
        name: String,
        chainId: Int,
        pairAddress: String,
        owner: String,
        spender: String,
        value: BigInteger,
        nonce: BigInteger,
        deadline: BigInteger
    ): String? {
        val data = PermitData()
        data.domain.name = name
        data.domain.version = "1"
        data.domain.chainId = chainId
        data.domain.verifyingContract = pairAddress
        data.message.owner = owner
        data.message.spender = spender
        data.message.value = value
        data.message.nonce = Numeric.toHexStringWithPrefix(nonce)
        data.message.deadline = deadline.toLong()
        return Gson().toJson(data)
    }

    override fun onCleared() {
        disposable.dispose()
    }

    private fun isSelectBSC(): Boolean {
        return selectedTab == LiquidityListModule.Tab.BSC
    }

}

data class LiquidityUiState(
    val liquidityViewItems: List<LiquidityViewItem>,
    val viewState: ViewState,
    val isRefreshing: Boolean,
    val selectPercent: Int,
    val amountCaution: HSCaution? = null
)


/*private class PermitData {
    var types = Types()
    var primaryType = "Permit"
    var domain = Domain()
    var message = Message()

    class Domain {
        var name: String? = null
        var version: String? = null
        var chainId: Int? = null
        var verifyingContract: String? = null
    }

    class Message {
        var owner: String? = null
        var spender: String? = null
        var value: BigInteger? = null
        var nonce: String? = null
        var deadline: Long = 0
    }

    class Types {
        var eIP712Domain = arrayOf(
            TypeDefine("name", "string"),
            TypeDefine("version", "string"),
            TypeDefine("chainId", "uint256"),
            TypeDefine("verifyingContract", "address")
        )
        var permit = arrayOf(
            TypeDefine("owner", "address"),
            TypeDefine("spender", "address"),
            TypeDefine("value", "uint256"),
            TypeDefine("nonce", "uint256"),
            TypeDefine("deadline", "uint256")
        )
    }

    class TypeDefine(var name: String, var type: String)
}*/
