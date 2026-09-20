package io.horizontalsystems.bankwallet.modules.txsource

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tencent.mmkv.MMKV
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.EvmSyncSourceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * EVM 交易数据源设置：用户在 EtherScan 与 Chainstack 之间二选一。
 *
 * - EtherScan：使用各链浏览器 API 同步交易历史（原有方式）
 * - Chainstack：使用 Chainstack RPC 节点做区块扫描同步交易，不依赖浏览器 API
 *
 * 选择结果全局生效（所有 EVM 链统一适用），持久化到 MMKV。
 */
class EvmTransactionViewModel : ViewModel() {

    companion object {
        private const val ID_ETHERSCAN = 0
        private const val ID_CHAINSTACK = 1

        /** 当前是否使用 Chainstack 作为交易数据源（全局开关） */
        fun isChainstackSelected(): Boolean {
            return MMKV.defaultMMKV()?.decodeInt(
                EvmSyncSourceManager.KEY_TRANSACTION_SOURCE, ID_ETHERSCAN
            ) == ID_CHAINSTACK
        }
    }

    private var initSyncSource = sourceId(isChainstackSelected())
    private var currentSyncSource = initSyncSource
    private val isSaving = AtomicBoolean(false)

    var closeScreen by mutableStateOf(false)
        private set

    var viewState by mutableStateOf(
        ViewState(
            defaultItems = viewItems(),
            saveButtonEnabled = false,
            saving = false
        )
    )
        private set

    private fun sourceId(chainstack: Boolean) = if (chainstack) ID_CHAINSTACK else ID_ETHERSCAN

    private fun viewItems(): List<ViewItem> {
        return listOf(
            ViewItem(
                id = ID_ETHERSCAN,
                name = R.string.TransactionSource_Etherscan,
                selected = ID_ETHERSCAN == currentSyncSource
            ),
            ViewItem(
                id = ID_CHAINSTACK,
                name = R.string.TransactionSource_Chainstack,
                selected = ID_CHAINSTACK == currentSyncSource
            )
        )
    }

    private fun syncState() {
        viewState = ViewState(
            defaultItems = viewItems(),
            saveButtonEnabled = initSyncSource != currentSyncSource,
            saving = isSaving.get()
        )
    }

    fun onSelectSyncSource(syncSource: Int) {
        if (currentSyncSource == syncSource) return
        currentSyncSource = syncSource
        syncState()
    }

    /**
     * 保存并应用：记录全局选择后，遍历所有已支持 EVM 链的数据源选项，
     * 统一切换到用户选择的数据源，并触发适配器重建与数据重新同步。
     */
    fun save() {
        if (!isSaving.compareAndSet(false, true)) return
        val useChainstack = currentSyncSource == ID_CHAINSTACK
        syncState()

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // 内部会持久化全局开关并通知各链重新读取同步源
                App.evmSyncSourceManager.saveTransactionSourceForAllChains(useChainstack)
            }
            isSaving.set(false)
            closeScreen = true
        }
    }

    data class ViewItem(
        val id: Int,
        val name: Int,
        val selected: Boolean,
    )

    data class ViewState(
        val defaultItems: List<ViewItem>,
        val saveButtonEnabled: Boolean,
        val saving: Boolean
    )
}
