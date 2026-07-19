package io.horizontalsystems.bankwallet.modules.safe4.dapp

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.parcelize.Parcelize

object Safe4DAppModule {

    private val sharedService = Safe4DAppService()

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return Safe4DAppViewModel(sharedService) as T
        }
    }

    class FactoryRegister(private val input: RegisterInput) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return Safe4DAppRegisterViewModel(sharedService, input) as T
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
        val descError: Int? = null
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
