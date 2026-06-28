package io.horizontalsystems.bankwallet.modules.blockchainsettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.order
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.safe4.SafeInfoManager
import io.horizontalsystems.bankwallet.modules.safe4.node.LockRecordManager
import io.horizontalsystems.bankwallet.modules.safe4.src20.SRCLockManager
import io.horizontalsystems.marketkit.SafeExtend
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class BlockchainSettingsViewModel(
    private val service: BlockchainSettingsService,
    private val localStorage: ILocalStorage,
) : ViewModel() {

    var btcLikeChains by mutableStateOf<List<BlockchainSettingsModule.BlockchainViewItem>>(listOf())
        private set

    var otherChains by mutableStateOf<List<BlockchainSettingsModule.BlockchainViewItem>>(listOf())
        private set

    private var _isSafe4TestNet by mutableStateOf(localStorage.isSafe4TestNet)

    var isSafe4TestNet: Boolean
        get() = _isSafe4TestNet
        set(value) {
            if (_isSafe4TestNet != value) {
                // 切换网络前，先取消所有正在进行的锁仓同步任务，
                // 防止旧链数据被写入新链的数据库分区
                LockRecordManager.cancelAllSyncTasks()
                SRCLockManager.cancelSync()

                localStorage.isSafe4TestNet = value
                SafeExtend.isSafe4TestNet = value
                App.evmBlockchainManager.resyncSafeFour()
                SafeInfoManager.startNet()
                _isSafe4TestNet = value

                // 重新启动锁仓同步任务(使用新链)
                LockRecordManager.switchNetwork()
            }
        }

    init {
        viewModelScope.launch {
            service.blockchainItemsObservable.asFlow().collect {
                sync(it)
            }
        }

        service.start()
        sync(service.blockchainItems)
    }

    override fun onCleared() {
        service.stop()
    }

    private fun sync(blockchainItems: List<BlockchainSettingsModule.BlockchainItem>) {
        viewModelScope.launch {
            val btcItems = blockchainItems
                .filterIsInstance<BlockchainSettingsModule.BlockchainItem.Btc>()
                .map { item ->
                    BlockchainSettingsModule.BlockchainViewItem(
                        title = item.blockchain.coinName,
                        subtitle = Translator.getString(item.restoreMode.title),
                        imageUrl = item.blockchain.type.imageUrl,
                        blockchainItem = item
                    )
                }
            val moneroItems = blockchainItems
                .filterIsInstance<BlockchainSettingsModule.BlockchainItem.Monero>()
                .map { item ->
                    BlockchainSettingsModule.BlockchainViewItem(
                        title = item.blockchain.name,
                        subtitle = item.node.name,
                        imageUrl = item.blockchain.type.imageUrl,
                        blockchainItem = item
                    )
                }
            btcLikeChains = (btcItems + moneroItems).sortedBy { it.blockchainItem.blockchain.type.order }

            otherChains = blockchainItems
                .filterNot { it is BlockchainSettingsModule.BlockchainItem.Btc }
                .mapNotNull { item ->
                    when (item) {
                        is BlockchainSettingsModule.BlockchainItem.Evm -> BlockchainSettingsModule.BlockchainViewItem(
                            title = item.blockchain.name,
                            subtitle = item.syncSource.name,
                            imageUrl = item.blockchain.type.imageUrl,
                            blockchainItem = item
                        )
                        is BlockchainSettingsModule.BlockchainItem.Solana -> BlockchainSettingsModule.BlockchainViewItem(
                            title = item.blockchain.name,
                            subtitle = item.rpcSource.name,
                            imageUrl = item.blockchain.type.imageUrl,
                            blockchainItem = item
                        )
                        else -> null
                    }
                }
        }
    }

}
