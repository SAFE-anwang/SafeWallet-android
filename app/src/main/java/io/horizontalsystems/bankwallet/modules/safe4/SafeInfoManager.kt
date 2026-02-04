package io.horizontalsystems.bankwallet.modules.safe4

import android.os.Parcelable
import android.util.Log
import com.anwang.safewallet.safekit.model.SafeInfo
import com.anwang.safewallet.safekit.model.SafeUsdtInfo
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.wsafekit.UsdtSafeManager
import io.horizontalsystems.wsafekit.WSafeManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.parcelize.Parcelize

object SafeInfoManager {

    private const val ID_SAFE = "ID_SAFE"
    private const val KEY_SAFE_INFO = "KEY_SAFE_INFO_2"
    private const val KEY_SAFE4_INFO = "KEY_SAFE4_INFO_2"
    private const val KEY_SAFE4_USDT_INFO = "KEY_SAFE4_USDT_INFO"

    private val safeStorage by lazy { MMKV.mmkvWithID(ID_SAFE, MMKV.SINGLE_PROCESS_MODE) }

    private val disposables = CompositeDisposable()

    fun startNet() {
        val chain: Chain
        if (App.evmBlockchainManager.getEvmKitManager(BlockchainType.Ethereum).evmKitWrapper != null) {
            if (App.evmBlockchainManager.getEvmKitManager(BlockchainType.Ethereum).evmKitWrapper?.evmKit != null) {
//                chain = App.ethereumKitManager.evmKitWrapper?.evmKit!!.chain
                chain = App.evmBlockchainManager.getEvmKitManager(BlockchainType.Ethereum).evmKitWrapper?.evmKit!!.chain
            } else {
                chain = Chain.Ethereum
            }
        } else {
            chain = Chain.Ethereum
        }
        val safeNetType = WSafeManager(chain).getSafeNetType()
        App.safeProvider.getSafeInfo(safeNetType)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                saveMmkv(it, false)
            }, {
                Log.e("safe4", "getSafeInfo error", it)
            })
            .let {
                disposables.add(it)
            }
        val safe4NetType = WSafeManager(chain, true).getSafeNetType()
        App.safeProvider.getSafeInfo(safe4NetType)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                saveMmkv(it, true)
            }, {
                Log.e("safe4", "getSafeInfo error", it)
            })
            .let {
                disposables.add(it)
            }
        val safe4UsdtNetType = UsdtSafeManager(Chain.SafeFour).getSafeNetType()
        App.safeProvider.getSafeUsdtInfo(safe4UsdtNetType)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Log.d("longwen", "usdt=$it")
                saveUsdtMmkv(it)
            }, {
                Log.e("safe4", "getSafeInfo error", it)
            })
            .let {
                disposables.add(it)
            }
    }

    fun saveMmkv(safeInfo: SafeInfo, isSafe4: Boolean) {
        val gson = Gson()
        val safeInfoJson = gson.toJson(safeInfo)
        val safeInfoPO = gson.fromJson(safeInfoJson, SafeInfoPO::class.java)
        safeStorage?.encode(getKey(isSafe4), safeInfoPO)
    }

    fun saveUsdtMmkv(safeInfo: SafeUsdtInfo) {
        val gson = Gson()
        val safeInfoJson = gson.toJson(safeInfo)
        val safeInfoPO = gson.fromJson(safeInfoJson, SafeUsdtInfoPO::class.java)
        safeStorage?.encode(getKey(false, true), safeInfoPO)
    }

    private fun getKey(isSafe4: Boolean, isUsdt: Boolean = false): String {
        return if (isSafe4) {
            KEY_SAFE4_INFO
        } else if (isUsdt) {
            KEY_SAFE4_USDT_INFO
        } else {
            KEY_SAFE_INFO
        }
    }

    fun getSafeInfo(isSafe4: Boolean): SafeInfoPO {
        var safeInfoPO = safeStorage?.decodeParcelable(getKey(isSafe4), SafeInfoPO::class.java)
        if(safeInfoPO == null){
            val chain: Chain
            if (App.evmBlockchainManager.getEvmKitManager(BlockchainType.Ethereum).evmKitWrapper != null) {
                if (App.evmBlockchainManager.getEvmKitManager(BlockchainType.Ethereum).evmKitWrapper?.evmKit != null) {
                    chain = App.evmBlockchainManager.getEvmKitManager(BlockchainType.Ethereum).evmKitWrapper?.evmKit!!.chain
                } else {
                    chain = Chain.Ethereum
                }
            } else {
                chain = Chain.Ethereum
            }
            safeInfoPO = defaultSafeInfo(chain)
        }
        return safeInfoPO
    }

    fun getSafeUsdtInfo(): SafeUsdtInfoPO {
        var safeInfoPO = safeStorage?.decodeParcelable(getKey(false, true), SafeUsdtInfoPO::class.java)
        if(safeInfoPO == null){
            val chain: Chain
            if (App.evmBlockchainManager.getEvmKitManager(BlockchainType.SafeFour).evmKitWrapper != null) {
                if (App.evmBlockchainManager.getEvmKitManager(BlockchainType.SafeFour).evmKitWrapper?.evmKit != null) {
                    chain = App.evmBlockchainManager.getEvmKitManager(BlockchainType.SafeFour).evmKitWrapper?.evmKit!!.chain
                } else {
                    chain = Chain.SafeFour
                }
            } else {
                chain = Chain.SafeFour
            }
            safeInfoPO = defaultSafeUsdtInfo(chain)
        }
        return safeInfoPO
    }

    fun clear() {
        disposables.clear()
    }

    private fun defaultSafeInfo(chain: Chain): SafeInfoPO {
        val mainNet = SafeInfoPO("", "2",
            ChainInfoPO("", "","0.25", safe2eth = true, eth2safe = true),
            ChainBscInfoPO("", "","0.25", safe2bsc = true, bsc2safe = true),
            MaticChainInfo("", "","0.25", safe2matic = true, matic2safe = true)
        )
        val testNet = SafeInfoPO("", "0.01",
            ChainInfoPO("", "","0", safe2eth = true, eth2safe = true),
            ChainBscInfoPO("", "","0.25", safe2bsc = true, bsc2safe = true),
            MaticChainInfo("", "","0.25", safe2matic = true, matic2safe = true)
        )
        val safeInfoPO = when (chain) {
            Chain.Ethereum -> mainNet
            Chain.EthereumRopsten -> testNet
            else -> throw WSafeManager.UnsupportedChainError.NoSafeNetType
        }
        return safeInfoPO
    }

    private fun defaultSafeUsdtInfo(chain: Chain): SafeUsdtInfoPO {
        val mainNet = SafeUsdtInfoPO("", "0.1",
            EthUsdtInfoPO("", "","0.25", safe2eth = true, eth2safe = true),
            BscUsdtInfoP0("", "","0.25", safe2bsc = true, bsc2safe = true),
            SolUsdtInfoP0("", "","0.25", safe2sol = false, sol2safe = false),
            TrxUsdtInfoP0("", "","0.25", safe2trx = false, trx2safe = false)
        )
        val testNet = SafeUsdtInfoPO("", "0.01",

            EthUsdtInfoPO("", "","0.25", safe2eth = true, eth2safe = true),
            BscUsdtInfoP0("", "","0.25", safe2bsc = true, bsc2safe = true),
            SolUsdtInfoP0("", "","0.25", safe2sol = false, sol2safe = false),
            TrxUsdtInfoP0("", "","0.25", safe2trx = false, trx2safe = false)
        )
        val safeInfoPO = if (Chain.SafeFour.isSafe4TestNetId) {
            testNet
        } else {
            mainNet
        }
        return safeInfoPO
    }

    @Parcelize
    data class SafeInfoPO(
        val safe_usdt: String,
        val minamount: String,
        val eth: ChainInfoPO,
        val bsc: ChainBscInfoPO,
        val matic: MaticChainInfo
    ) : Parcelable

    @Parcelize
    data class ChainInfoPO(
        val price: String,
        val gas_price_gwei: String,
        val safe_fee: String,
        val safe2eth: Boolean,
        val eth2safe: Boolean
    )  : Parcelable

    @Parcelize
    data class ChainBscInfoPO(
        val price: String,
        val gas_price_gwei: String,
        val safe_fee: String,
        val safe2bsc: Boolean,
        val bsc2safe: Boolean
    )  : Parcelable

    @Parcelize
    data class MaticChainInfo(
        val price: String,
        val gas_price_gwei: String,
        val safe_fee: String,
        val safe2matic: Boolean,
        val matic2safe: Boolean
    )  : Parcelable

    @Parcelize
    data class SafeUsdtInfoPO(
        val safe_usdt: String,
        val minamount: String,
        val eth: EthUsdtInfoPO,
        val bsc: BscUsdtInfoP0,
        val sol: SolUsdtInfoP0,
        val trx: TrxUsdtInfoP0
    ) : Parcelable

    @Parcelize
    data class EthUsdtInfoPO(
        val price: String,
        val gas_price_gwei: String,
        val usdt_fee: String,
        val safe2eth: Boolean,
        val eth2safe: Boolean
    )  : Parcelable

    @Parcelize
    data class BscUsdtInfoP0(
        val price: String,
        val gas_price_gwei: String,
        val usdt_fee: String,
        val safe2bsc: Boolean,
        val bsc2safe: Boolean
    )  : Parcelable

    @Parcelize
    data class SolUsdtInfoP0(
        val price: String,
        val gas_price_gwei: String,
        val usdt_fee: String,
        val safe2sol: Boolean,
        val sol2safe: Boolean
    )  : Parcelable

    @Parcelize
    data class TrxUsdtInfoP0(
        val price: String,
        val gas_price_gwei: String,
        val usdt_fee: String,
        val safe2trx: Boolean,
        val trx2safe: Boolean
    )  : Parcelable

}