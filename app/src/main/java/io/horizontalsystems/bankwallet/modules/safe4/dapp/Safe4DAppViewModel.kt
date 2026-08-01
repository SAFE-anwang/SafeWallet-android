package io.horizontalsystems.bankwallet.modules.safe4.dapp

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class Safe4DAppViewModel(
    private val service: Safe4DAppService
) : ViewModel() {

    private val _uiState = MutableStateFlow(Safe4DAppModule.UiState())
    val uiState: StateFlow<Safe4DAppModule.UiState> = _uiState.asStateFlow()

    private val disposables = CompositeDisposable()

    init {
        service.getDAppsObservable()
            .subscribeIO { dApps ->
                _uiState.value = _uiState.value.copy(
                    dApps = dApps,
                    isLoading = false,
                    error = null
                )
            }
            .let { disposables.add(it) }

        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        service.refresh()
    }

    fun removeDApp(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                service.removeDApp(id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun requestDeleteDApp(dapp: ManagedDAppItem) {
        _uiState.value = _uiState.value.copy(showDeleteConfirmation = dapp)
    }

    fun dismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmation = null)
    }

    fun confirmDeleteDApp() {
        val dapp = _uiState.value.showDeleteConfirmation ?: return
        _uiState.value = _uiState.value.copy(showDeleteConfirmation = null)
        removeDApp(dapp.id)
    }

    fun getService(): Safe4DAppService = service

    override fun onCleared() {
        disposables.clear()
        service.clear()
    }
}

class Safe4DAppRegisterViewModel(
    private val service: Safe4DAppService,
    private val input: Safe4DAppModule.RegisterInput
) : ViewModel() {

    val wallet: Wallet? get() = service.getActiveSafe4Wallet()

    private val _registerState = MutableStateFlow(Safe4DAppModule.RegisterUiState())
    val registerState: StateFlow<Safe4DAppModule.RegisterUiState> = _registerState.asStateFlow()

    private val _sendResult = MutableStateFlow<SendResult?>(null)
    val sendResultFlow = _sendResult.asStateFlow()
    var sendResult: SendResult?
        get() = _sendResult.value
        set(value) { _sendResult.value = value }

    val canSubmit: Boolean
        get() {
            val state = _registerState.value
            // URL must not have an existence error
            if (state.urlError != null) return false
            if (state.isEditing) {
                val original = input.existingDApp ?: return false
                // Must have name (>=8 chars) and url, and at least one field changed
                return state.name.length >= 8 &&
                        state.url.isNotBlank() &&
                        (state.name != original.name ||
                        state.url != original.url ||
                        state.description != original.description ||
                        state.iconUrl != original.iconUrl ||
                        state.contractAddr != original.contractAddr ||
                        state.officialUrl != original.officialUrl ||
                        state.officialEmail != original.officialEmail ||
                        state.officialAccount != original.officialAccount ||
                        state.keyword != original.keyword)
            }
            // Register: name >= 8 chars, desc >= 12 chars, url and contractAddr not blank
            return state.name.length >= 8 &&
                    state.url.isNotBlank() &&
                    state.description.length >= 12 &&
                    state.contractAddr.isNotBlank()
        }

    init {
        input.existingDApp?.let { dapp ->
            _registerState.value = Safe4DAppModule.RegisterUiState(
                name = dapp.name,
                url = dapp.url,
                description = dapp.description,
                iconUrl = dapp.iconUrl,
                contractAddr = dapp.contractAddr,
                officialUrl = dapp.officialUrl,
                officialEmail = dapp.officialEmail,
                officialAccount = dapp.officialAccount,
                keyword = dapp.keyword,
                isEditing = true
            )
            // Load existing logo from cache/chain for edit mode
            loadLogo(dapp.id)
        } ?: run {
            // Load draft for new registration
            val draft = service.loadDraftDApp()
            if (draft != null) {
                _registerState.value = draft
            }
        }
    }

    private fun saveDraft() {
        val state = _registerState.value
        if (!state.isEditing) {
            service.saveDraftDApp(state)
        }
    }

    fun updateName(name: String) {
        _registerState.value = _registerState.value.copy(
            name = name,
            nameError = when {
                name.isEmpty() -> null
                name.length < 8 -> R.string.Safe4_DApp_Name_TooShort
                else -> null
            }
        )
        saveDraft()
    }

    fun updateUrl(url: String) {
        _registerState.value = _registerState.value.copy(url = url, urlError = null)
        saveDraft()
        // Check URL uniqueness in background (skip if editing and URL unchanged from original)
        checkUrlUniqueness(url)
    }

    private var urlCheckJob: kotlinx.coroutines.Job? = null

    private fun checkUrlUniqueness(url: String) {
        urlCheckJob?.cancel()
        // Skip if editing and URL matches the original DApp's URL
        val originalUrl = input.existingDApp?.url.orEmpty()
        if (url.isBlank() || url == originalUrl) return

        urlCheckJob = viewModelScope.launch(Dispatchers.IO) {
            // Debounce: only check after 500ms of no input
            kotlinx.coroutines.delay(500)
            try {
                val exists = service.isRunUrlExists(url)
                _registerState.value = _registerState.value.copy(
                    urlError = if (exists) R.string.Safe4_DApp_URL_Exists else null
                )
            } catch (e: Exception) {
                Log.d("Safe4DAppRegisterViewModel", "URL existence check failed: ${e.message}")
            }
        }
    }

    fun updateDescription(desc: String) {
        _registerState.value = _registerState.value.copy(
            description = desc,
            descError = when {
                desc.isEmpty() -> null
                desc.length < 12 -> R.string.Safe4_DApp_Desc_TooShort
                else -> null
            }
        )
        saveDraft()
    }

    fun updateIconUrl(iconUrl: String) {
        _registerState.value = _registerState.value.copy(iconUrl = iconUrl)
        saveDraft()
    }

    fun updateContractAddr(contractAddr: String) {
        _registerState.value = _registerState.value.copy(contractAddr = contractAddr)
        saveDraft()
    }

    fun updateContractAddr(address: Address?) {
        _registerState.value = _registerState.value.copy(contractAddr = address?.hex ?: "")
        saveDraft()
    }

    fun updateOfficialUrl(officialUrl: String) {
        _registerState.value = _registerState.value.copy(officialUrl = officialUrl)
        saveDraft()
    }

    fun updateOfficialEmail(officialEmail: String) {
        _registerState.value = _registerState.value.copy(officialEmail = officialEmail)
        saveDraft()
    }

    fun updateOfficialAccount(officialAccount: String) {
        _registerState.value = _registerState.value.copy(officialAccount = officialAccount)
        saveDraft()
    }

    fun updateOfficialAccount(address: Address?) {
        _registerState.value = _registerState.value.copy(officialAccount = address?.hex ?: "")
        saveDraft()
    }

    fun updateKeyword(keyword: String) {
        _registerState.value = _registerState.value.copy(keyword = keyword)
        saveDraft()
    }

    // Logo related state
    var logoBytes by mutableStateOf<ByteArray?>(null)
    var logoPayAmount by mutableStateOf<String?>(null)
    var hasNewLogo by mutableStateOf(false)
    private val _logoResult = MutableStateFlow<SendResult?>(null)
    val logoResultFlow = _logoResult.asStateFlow()
    var logoResult: SendResult?
        get() = _logoResult.value
        set(value) { _logoResult.value = value }

    fun loadLogoPayAmount() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val amount = service.getLogoPayAmount()
                val decimals = wallet?.token?.decimals ?: 18
                val converted = amount.toBigDecimal().movePointLeft(decimals).stripTrailingZeros()
                logoPayAmount = converted.toPlainString()
            } catch (e: Exception) {
                Log.d("Safe4DAppRegisterViewModel", "Failed to load logo pay amount: ${e.message}")
            }
        }
    }

    private fun loadLogo(dappId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Try cache first, then chain
                val bytes = service.getCachedLogoBytes(dappId)
                    ?: service.getLogo(dappId)
                if (bytes != null && bytes.isNotEmpty()) {
                    logoBytes = bytes
                }
                // Also load logo pay amount for display
                loadLogoPayAmount()
            } catch (e: Exception) {
                Log.d("Safe4DAppRegisterViewModel", "Failed to load logo: ${e.message}")
            }
        }
    }

    fun submitLogo() {
        val dappId = input.existingDApp?.id ?: run {
            logoResult = SendResult.Failed(HSCaution(TranslatableString.PlainString("No DApp to set logo for")))
            return
        }
        val bytes = logoBytes ?: run {
            logoResult = SendResult.Failed(HSCaution(TranslatableString.PlainString("No logo selected")))
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            logoResult = SendResult.Sending
            try {
                service.setLogo(dappId, bytes)
                // Cache logo locally so it can be loaded immediately next time
                service.cacheLogoForId(dappId, bytes)
                // Keep showing the uploaded logo (don't clear bytes)
                logoResult = SendResult.Sent()
                hasNewLogo = false
            } catch (e: Exception) {
                Log.d("Safe4DAppRegisterViewModel", "setLogo failed: ${e.message}")
                logoResult = SendResult.Failed(HSCaution(TranslatableString.PlainString(e.message ?: "Failed to set logo")))
            }
        }
    }

    fun submit() {
        val state = _registerState.value

        var hasError = false
        var nameError: Int? = null
        var descError: Int? = null

        if (state.name.length < 8) {
            nameError = R.string.Safe4_DApp_Name_TooShort
            hasError = true
        }
        if (!state.isEditing && state.description.length < 12) {
            descError = R.string.Safe4_DApp_Desc_TooShort
            hasError = true
        }
        if (state.url.isBlank()) {
            hasError = true
        }

        if (hasError) {
            _registerState.value = state.copy(nameError = nameError, descError = descError)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            sendResult = SendResult.Sending
            try {
                if (state.isEditing && input.existingDApp != null) {
                    service.updateDApp(
                        id = input.existingDApp.id,
                        name = state.name,
                        url = state.url,
                        description = state.description,
                        iconUrl = state.iconUrl,
                        contractAddr = state.contractAddr,
                        officialUrl = state.officialUrl,
                        officialEmail = state.officialEmail,
                        officialAccount = state.officialAccount,
                        keyword = state.keyword,
                        existing = input.existingDApp
                    )
                } else {
                    service.registerDApp(
                        name = state.name,
                        url = state.url,
                        description = state.description,
                        iconUrl = state.iconUrl,
                        contractAddr = state.contractAddr,
                        officialUrl = state.officialUrl,
                        officialEmail = state.officialEmail
                    )
                    service.clearDraftDApp()
                }
                sendResult = SendResult.Sent()
            } catch (e: Exception) {
                Log.d("Safe4DAppRegisterViewModel", "submit failed: ${e.message}")
                sendResult = SendResult.Failed(HSCaution(TranslatableString.PlainString(e.message ?: "Operation failed")))
            }
        }
    }
}
