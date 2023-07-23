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
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IWalletStorage
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.Wallet
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
import org.web3j.utils.Convert
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
    private val web3j = Connect.connect()

    private val disposable = CompositeDisposable()

    private var liquidityItems = mutableListOf<LiquidityListModule.LiquidityItem>()
    private var liquidityViewItems = listOf<LiquidityViewItem>()
    private var viewState: ViewState = ViewState.Loading
    private var isRefreshing = false

    var uiState by mutableStateOf(
        LiquidityUiState(
            liquidityViewItems = liquidityViewItems,
            viewState = viewState,
            true
        )
    )
        private set

    val removeErrorMessage = SingleLiveEvent<String?>()

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
        getAllLiquidity()
    }

    fun refresh() {
        getAllLiquidity()
    }

    private fun getTokenEntity(uids: List<String>, type: String):List<TokenEntity> {
        return marketKit.getTokenEntity(uids, type)
    }

    private fun getAllLiquidity() {
        viewState = ViewState.Loading
        val activeWallets = accountManager.activeAccount?.let {
            storage.wallets(it).filter {
                it.token.blockchainType is BlockchainType.BinanceSmartChain
                        && it.token.coin.code != "Cake-LP"
            }
        } ?: listOf()
        if (activeWallets.isEmpty()) return
        val uids = activeWallets.map { it.coin.uid }
        val tokenEntityList = getTokenEntity(uids, "binance-smart-chain")
        liquidityItems.clear()
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing = true
            emitState()
            val list = mutableListOf<LiquidityViewItem>()
            getAllPair(activeWallets).forEach { pair ->
                val tokenEntityA = tokenEntityList.find { it.coinUid == pair.first.coin.uid }?.reference ?: getWethAddress(pair.first.token.blockchain.type)
                val tokenEntityB = tokenEntityList.find { it.coinUid == pair.second.coin.uid }?.reference ?: getWethAddress(pair.second.token.blockchain.type)
                if (tokenEntityA != null && tokenEntityB != null) {
                    try {
                        getLiquidity(pair.first, tokenEntityA, pair.second, tokenEntityB)?.let {
                            liquidityItems.add(it)
                            list.add(liquidityViewItemFactory.viewItem(it))
                        }
                    } catch (e: Exception) {
                        viewState = ViewState.Error(e)
                    }
                }
            }
            viewState = ViewState.Success
            liquidityViewItems = list.map { it }
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

    private fun getLiquidity(walletA: Wallet, tokenAAddress: String, walletB: Wallet, tokenBAddress: String): LiquidityListModule.LiquidityItem? {
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
        val pair = LiquidityPair.getPairReservesForPancakeSwap(web3j, tokenA, tokenB) ?: return null

        val pairAddress = pair.get(0) as String
        val token0 = pair.get(1) as Token
        val token1 = pair.get(2) as Token

        val r0: BigInteger = pair[3] as BigInteger
        val r1: BigInteger = pair[4] as BigInteger

        // 查询 流动性代币的数量
        val poolTokenTotalSupply = TotalSupply.getTotalSupply(web3j, pairAddress)
        // 查询 当前用户拥有的流动性代币数量
        val balanceOfAccount = BalanceOf.balanceOf(web3j, pairAddress, adapterA.receiveAddress)
        Log.d("Pool Token TotalSupply = {}", "$poolTokenTotalSupply")
        Log.d("BalanceOf {} = {}", "${adapterA.receiveAddress}, ${balanceOfAccount}")
        // 计算用户在池子中的流动性占比
        val shareRate = BigDecimal(balanceOfAccount).divide(
            BigDecimal(poolTokenTotalSupply), 18, RoundingMode.DOWN
        )
        Log.d(
            "用户流动性占比:{}",
            shareRate.multiply(BigDecimal("100")).toString() + "%"
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
                walletA,
                walletB,
                tokenAAddress,
                tokenBAddress,
                pooledR0Amount,
                pooledR1Amount,
                balanceOfAccount,
                shareRate,
                poolTokenTotalSupply
            )
    }

    private fun emitState() {
        val newUiState = LiquidityUiState(
            liquidityViewItems = liquidityViewItems,
            viewState = viewState,
            isRefreshing = isRefreshing
        )

        viewModelScope.launch {
            uiState = newUiState
        }
    }

    private fun removeItem(removeIndex: Int) {
        val list = mutableListOf<LiquidityViewItem>()
        liquidityViewItems.forEachIndexed{ index, liquidityViewItem ->
            if (removeIndex != index) {
                list.add(liquidityViewItem)
            }
        }
        liquidityViewItems = list
        emitState()
    }

    fun removeLiquidity(index: Int, walletA: Wallet, tokenAAddress: String, walletB: Wallet, tokenBAddress: String) {
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
                val pair =
                    LiquidityPair.getPairReservesForPancakeSwap(web3j, tokenA, tokenB) ?: return@launch
                val pairAddress = pair.get(0) as String
                val token0 = pair.get(1) as Token
                val token1 = pair.get(2) as Token

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
                    "${evmKitWrapper!!.evmKit.receiveAddress.hex}, ${balanceOfAccount}"
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


                // 假设用户期望移除抵押的 25% 的代币;
                val removeLiquidityAmount =
                    BigDecimal(balanceOfAccount).multiply(BigDecimal("1")).toBigInteger()
                // 将会得到的 token0 和 token1 的数量
                val removeToken0Amount = pooledR0Amount.multiply(BigDecimal("1"))
                val removeToken1Amount = pooledR1Amount.multiply(BigDecimal("1"))
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
                val json = buildSignJson(
                    name,
                    56,
                    pairAddress,
                    evmKitWrapper!!.evmKit.receiveAddress.hex,
                    Constants.DEX.PANCAKE_V2_ROUTER_ADDRESS,
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

                val hash = TransactionContractSend.send(
                    web3j, Credentials.create(evmKitWrapper.signer!!.privateKey.toString(16)),
                    Constants.DEX.PANCAKE_V2_ROUTER_ADDRESS,
                    encode,
                    BigInteger.ZERO, nonce,
                    Convert.toWei("10", Convert.Unit.GWEI).toBigInteger(),  // GAS PRICE : 5GWei
                    BigInteger("500000") // GAS LIMIT
                )
                // 0xab43576d55e54c3c51ecff56b030cd83945ec7ee0892539953a8ee467570a73d
                // 0xab43576d55e54c3c51ecff56b030cd83945ec7ee0892539953a8ee467570a73d
                Log.i("Execute Router.removeLiquidityWithPermit Hash = {}", hash)
                withContext(Dispatchers.Main) {
                    removeErrorMessage.value = if (App.languageManager.currentLanguage == "zh") {
                        "移除成功"
                    } else {
                        "Remove Success"
                    }
                    removeItem(index)
                }
                refresh()
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


}

data class LiquidityUiState(
    val liquidityViewItems: List<LiquidityViewItem>,
    val viewState: ViewState,
    val isRefreshing: Boolean
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
