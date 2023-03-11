package io.horizontalsystems.bankwallet.modules.safe4

import android.os.Parcelable
import android.util.Log
import com.anwang.safewallet.safekit.model.SafeInfo
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.wsafekit.WSafeManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.parcelize.Parcelize

object SafeInfoManager {

    private const val ID_SAFE = "ID_SAFE"
    private const val KEY_SAFE_INFO = "KEY_SAFE_INFO"

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
                saveMmkv(it)
            }, {
                Log.e("safe4", "getSafeInfo error", it)
            })
            .let {
                disposables.add(it)
            }
    }

    fun saveMmkv(safeInfo: SafeInfo) {
        val gson = Gson()
        val safeInfoJson = gson.toJson(safeInfo)
        val safeInfoPO = gson.fromJson(safeInfoJson, SafeInfoPO::class.java)
        safeStorage?.encode(KEY_SAFE_INFO, safeInfoPO)
    }

    fun getSafeInfo(): SafeInfoPO {
        var safeInfoPO = safeStorage?.decodeParcelable(KEY_SAFE_INFO, SafeInfoPO::class.java)
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

    fun clear() {
        disposables.clear()
    }

    private fun defaultSafeInfo(chain: Chain): SafeInfoPO {
        val mainNet = SafeInfoPO("", "2",
            ChainInfoPO("", "","0.25", safe2eth = true, eth2safe = true),
            ChainInfoPO("", "","0.25", safe2eth = true, eth2safe = true),
            MaticChainInfo("", "","0.25", safe2matic = true, matic2safe = true)
        )
        val testNet = SafeInfoPO("", "0.01",
            ChainInfoPO("", "","0", safe2eth = true, eth2safe = true),
            ChainInfoPO("", "","0", safe2eth = true, eth2safe = true),
            MaticChainInfo("", "","0.25", safe2matic = true, matic2safe = true)
        )
        val safeInfoPO = when (chain) {
            Chain.Ethereum -> mainNet
            Chain.EthereumRopsten -> testNet
            else -> throw WSafeManager.UnsupportedChainError.NoSafeNetType
        }
        return safeInfoPO
    }

    @Parcelize
    data class SafeInfoPO(
        val safe_usdt: String,
        val minamount: String,
        val eth: ChainInfoPO,
        val bsc: ChainInfoPO,
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
    data class MaticChainInfo(
        val price: String,
        val gas_price_gwei: String,
        val safe_fee: String,
        val safe2matic: Boolean,
        val matic2safe: Boolean
    )  : Parcelable

}