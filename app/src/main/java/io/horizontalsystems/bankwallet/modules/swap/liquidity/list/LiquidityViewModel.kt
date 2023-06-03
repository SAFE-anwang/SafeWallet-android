package io.horizontalsystems.bankwallet.modules.swap.liquidity.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.ViewState
import io.reactivex.disposables.CompositeDisposable

class LiquidityViewModel(
    private val service: LiquidityService,
    private val clearables: List<Clearable>
) : ViewModel() {
    val disposable = CompositeDisposable()

    private var viewState: ViewState = ViewState.Loading
    private var liquidityViewItems = listOf<LiquidityViewItem>()
    private var isRefreshing = false

    val saveEnabledLiveData = MutableLiveData<Boolean>()
    val liquidityViewItemsLiveData = MutableLiveData<List<LiquidityViewItem>>()

    var uiState by mutableStateOf(
        LiquidityUiState(
            liquidityViewItems = liquidityViewItems,
            viewState = viewState,
            isRefreshing = isRefreshing,
        )
    )
        private set

    fun onItem(viewItem: LiquidityViewItem) {
        /*viewModelScope.launch {
            expandedWallet = when {
                viewItem.wallet == expandedWallet -> null
                else -> viewItem.wallet
            }

            service.balanceItemsFlow.value?.let { refreshViewItems(it) }
        }*/
    }

    fun onRefresh() {

    }


    data class LiquidityUiState(
        val liquidityViewItems: List<LiquidityViewItem>,
        val viewState: ViewState,
        val isRefreshing: Boolean
    )

}