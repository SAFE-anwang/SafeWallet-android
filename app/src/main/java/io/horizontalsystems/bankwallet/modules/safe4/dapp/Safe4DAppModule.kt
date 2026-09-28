package io.horizontalsystems.bankwallet.modules.safe4.dapp

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.parcelize.Parcelize

object Safe4DAppModule {

    @Volatile
    private var service: Safe4DAppService? = null

    /**
     * 进程内共享的 dApp 服务。
     *
     * 懒创建：dApp 页面从未打开时不会初始化（其构造会触发链上同步），
     * 同时保证链切换时能通过 [onChainChanged] 重置同一个实例。
     */
    @Synchronized
    fun sharedService(): Safe4DAppService =
        service ?: Safe4DAppService().also { service = it }

    /** 链切换监听者（如 dApp 浏览器列表），用于重新拉取链上数据 */
    private val chainChangeListeners = mutableListOf<() -> Unit>()

    fun addChainChangeListener(listener: () -> Unit) {
        synchronized(chainChangeListeners) {
            if (!chainChangeListeners.contains(listener)) {
                chainChangeListeners.add(listener)
            }
        }
    }

    fun removeChainChangeListener(listener: () -> Unit) {
        synchronized(chainChangeListeners) {
            chainChangeListeners.remove(listener)
        }
    }

    /**
     * 测试网 / 主网切换时调用，重置 dApp 缓存与链绑定，并通知监听者刷新。
     * 服务尚未初始化时无需处理。
     */
    fun onChainChanged() {
        service?.onChainChanged()
        val listeners = synchronized(chainChangeListeners) { chainChangeListeners.toList() }
        listeners.forEach { it() }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return Safe4DAppViewModel(sharedService()) as T
        }
    }

    class FactoryRegister(private val input: RegisterInput) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return Safe4DAppRegisterViewModel(sharedService(), input) as T
        }
    }

    @Parcelize
    data class RegisterInput(
        val existingDApp: ManagedDAppItem? = null,
        val walletAddress: String = ""
    ) : Parcelable

    data class UiState(
        val dApps: List<ManagedDAppItem> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val showDeleteConfirmation: ManagedDAppItem? = null
    )

    data class RegisterUiState(
        val name: String = "",
        val url: String = "",
        val description: String = "",
        val iconUrl: String = "",
        val contractAddr: String = "",
        val officialUrl: String = "",
        val officialEmail: String = "",
        val officialAccount: String = "",
        val keyword: String = "",
        val isEditing: Boolean = false,
        val nameError: Int? = null,
        val descError: Int? = null,
        val urlError: Int? = null
    )
}

@Parcelize
data class ManagedDAppItem(
    val id: String,
    val name: String,
    val url: String,
    val description: String,
    val category: String,
    val iconUrl: String,
    val contractAddr: String = "",
    val officialUrl: String = "",
    val officialEmail: String = "",
    val officialAccount: String = "",
    val keyword: String = "",
    val status: String,
    val fraudNum: Long = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val walletAddress: String
) : Parcelable
