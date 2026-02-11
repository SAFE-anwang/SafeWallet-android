package io.horizontalsystems.bankwallet.modules.safe4.safeprice

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.lifecycle.viewModelScope
import com.github.mikephil.charting.data.CandleEntry
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.CustomToken
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory
import io.horizontalsystems.bankwallet.modules.safe4.src20.DeployType
import io.horizontalsystems.bankwallet.modules.safe4.src20.SRC20Service
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchainSafe4
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigInteger

class SRC20InfoViewModel(
    val wallet: Wallet?
) : ViewModelUiState<ChartUiState>() {

    private val src20InfoService by lazy { SRC20InfoService() }

    private var marketPriceData: List<MarketPrice>? = null
    private var contactAddress: String = ""
    private var customToken: CustomToken? = null
    private var totalSupply: BigInteger? = BigInteger.ZERO
    private var description: String? = ""

    var rpcBlockchainSafe4: RpcBlockchainSafe4? = null

    var service: SRC20Service? = null

    init {
        wallet?.let {
            val adapter =
                (App.adapterManager.getAdapterForWallet(wallet) as? ISendEthereumAdapter)
            rpcBlockchainSafe4 = adapter?.evmKitWrapper?.evmKit?.blockchain as RpcBlockchainSafe4
            contactAddress = (wallet.token.type as TokenType.Eip20).address
            Log.d("SRC20InfoViewModel", "contract=$contactAddress")
        }


        src20InfoService.itemsObservable.subscribeOn(Schedulers.io())
            .subscribe({
                marketPriceData = it
                emitState()
            }, {

            })
        viewModelScope.launch {
            getTokenInfo()
            src20InfoService.getPrice()
        }
    }

    private fun getTokenInfo() {
        Log.d("SRC20InfoViewModel", "${App.appDatabase.customTokenDao().getAll()}")
        customToken = App.appDatabase.customTokenDao().getToken(Address(contactAddress).eip55)
        customToken?.let { customToken ->
            rpcBlockchainSafe4?.let {
                service = SRC20Service(customToken.getDeployType(), it.web3j, contactAddress)
            }
            description = service?.description(customToken.type)
            totalSupply = service?.totalSupply(customToken.type)
            emitState()
        }
    }

    override fun createState(): ChartUiState {
        return ChartUiState(
            marketPriceData,
            customToken,
            totalSupply?.let { NodeCovertFactory.formatSafe(it, customToken?.decimals?.toInt() ?: 18, customToken?.symbol ?: "SAFE") },
            description
        )
    }

}


data class ChartUiState(
    val price: List<MarketPrice>? = null,
    val customToken: CustomToken?,
    val totalSupply: String?,
    val description: String?
)