package io.horizontalsystems.bankwallet.modules.safe4.src20

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.safe4.CustomToken
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SRC20ManagerViewModel(
    val service: SRC20Service,
    private val tokensService: SyncSafe4TokensService
) : ViewModelUiState<DeployManagerUiState>() {

    private val disposables = CompositeDisposable()

    private var managerList: List<MangerItem>? = null

    init {
        tokensService.customTokenObservable
            .subscribeIO {
                it?.let {
                    managerList = handlerToken(it)
                }
                emitState()
            }?.let {
                disposables.add(it)
            }
        viewModelScope.launch(Dispatchers.IO) {
            tokensService.getCache()
            tokensService.getTokens()
        }
    }

    private fun handlerToken(list: List<CustomToken>): List<MangerItem> {
        return list.map {
            MangerItem(
                it,
                canAdditionalIssuance = it.getTypeForVersion() == DeployType.SRC20Mintable || it.getTypeForVersion() == DeployType.SRC20Burnable,
                canDestroy = it.getTypeForVersion() == DeployType.SRC20Burnable
            )
        }
    }

    override fun createState(): DeployManagerUiState {
        return DeployManagerUiState(
            managerList
        )
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}