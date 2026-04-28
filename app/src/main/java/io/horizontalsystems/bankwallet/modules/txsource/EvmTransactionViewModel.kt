package io.horizontalsystems.bankwallet.modules.txsource

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tencent.mmkv.MMKV
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.AdapterManager
import io.horizontalsystems.bankwallet.core.managers.EvmSyncSourceManager
import io.horizontalsystems.bankwallet.core.managers.uris
import io.horizontalsystems.bankwallet.entities.EvmSyncSource
import io.horizontalsystems.ethereumkit.network.EtherscanService
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class EvmTransactionViewModel(
) : ViewModel() {
    companion object {
        const val TRANSACTION_SOURCE = "transaction_source"
    }
    private var initSyncSource: Int = 0
    private var currentSyncSource = initSyncSource

    var viewState by mutableStateOf(ViewState(emptyList(), false))
        private set

    var closeScreen by mutableStateOf(false)
        private set

    init {
        initSyncSource = MMKV.defaultMMKV()?.decodeInt(TRANSACTION_SOURCE, 0) ?: 0
        currentSyncSource = initSyncSource

        Log.d("currentSyncSource", "initSyncSource=$initSyncSource, ${MMKV.defaultMMKV()?.decodeInt(TRANSACTION_SOURCE, 0)}")
        syncState()
    }

    private fun syncState() {
        viewState = ViewState(
            defaultItems = viewItems(),
            initSyncSource != currentSyncSource
        )

    }

    private fun viewItems(): List<ViewItem> {
        return listOf(
            ViewItem(
                id = 0,
                name = R.string.TransactionSource_Etherscan,
                selected = 0 == currentSyncSource
            ),
            ViewItem(
                id = 1,
                name = R.string.TransactionSource_Proxy,
                selected = 1 == currentSyncSource
            )
        )
    }

    fun onSelectSyncSource(syncSource: Int) {
        if (currentSyncSource == syncSource) return
        currentSyncSource = syncSource

        syncState()
    }

    fun save() {
        MMKV.defaultMMKV()?.encode(TRANSACTION_SOURCE, currentSyncSource)
        Log.d("currentSyncSource", "$currentSyncSource, ${MMKV.defaultMMKV()?.decodeInt(TRANSACTION_SOURCE, -1)}")
        closeScreen = true
        if (currentSyncSource == 1) {
            EtherscanService.isUserProxy = true
        }
    }

    data class ViewItem(
        val id: Int,
        val name: Int,
        val selected: Boolean,
    )

    data class ViewState(
        val defaultItems: List<ViewItem>,
        val saveButtonEnabled: Boolean
    )
}
