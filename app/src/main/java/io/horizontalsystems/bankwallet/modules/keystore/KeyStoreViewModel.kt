package io.horizontalsystems.bankwallet.modules.keystore

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.KeystoreAuthLogger
import io.horizontalsystems.core.IKeyStoreManager
import io.horizontalsystems.core.security.KeyStoreValidationError
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class KeyStoreViewModel(
    private val keyStoreManager: IKeyStoreManager,
    mode: KeyStoreModule.ModeType
) : ViewModel() {

    var showSystemLockWarning by mutableStateOf(false)
        private set

    var showBiometricPrompt by mutableStateOf(false)
        private set

    var showInvalidKeyWarning by mutableStateOf(false)
        private set

    var showAuthRetryExceeded by mutableStateOf(false)
        private set

    var openMainModule by mutableStateOf(false)
        private set

    var closeApp by mutableStateOf(false)
        private set

    init {
        KeystoreAuthLogger.info("KeyStoreVM", "init | mode=$mode")
        when (mode) {
            KeyStoreModule.ModeType.NoSystemLock -> {
                showSystemLockWarning = true
                keyStoreManager.resetApp("NoSystemLock")
            }
            KeyStoreModule.ModeType.InvalidKey -> {
                showInvalidKeyWarning = true
                keyStoreManager.resetApp("InvalidKey")
            }
            KeyStoreModule.ModeType.UserAuthentication -> {
                showBiometricPrompt = true
            }
        }
    }

    fun onCloseInvalidKeyWarning() {
        keyStoreManager.removeKey()
        showInvalidKeyWarning = false
        openMainModule = true
    }

    fun onAuthenticationCanceled() {
        KeystoreAuthLogger.info("KeyStoreVM", "onAuthenticationCanceled → closeApp")
        showBiometricPrompt = false
        closeApp = true
    }

    fun onAuthenticationSuccess() {
        KeystoreAuthLogger.info("KeyStoreVM", "onAuthenticationSuccess | BiometricPrompt reported SUCCESS")
        showBiometricPrompt = false

        // Retry cipher operation after authentication with delay.
        // Keystore may not have processed the auth event immediately,
        // so we retry with increasing delays before giving up.
        viewModelScope.launch {
            val delays = listOf(300L, 1000L)
            var lastError: Exception? = null

            for (delayMs in delays) {
                delay(delayMs)
                try {
                    KeystoreAuthLogger.info("KeyStoreVM", "Calling keyStoreManager.validateKeyStore() after ${delayMs}ms delay...")
                    keyStoreManager.validateKeyStore()
                    KeystoreAuthLogger.info("KeyStoreVM", "validateKeyStore() PASSED after ${delayMs}ms delay → openMainModule")
                    openMainModule = true
                    return@launch
                } catch (e: Exception) {
                    KeystoreAuthLogger.info("KeyStoreVM", "validateKeyStore() failed after ${delayMs}ms delay | ${e.javaClass.simpleName}, retrying...")
                    lastError = e
                }
            }

            // All retries exhausted
            KeystoreAuthLogger.error("KeyStoreVM", "validateKeyStore() failed after all retries | ${lastError?.javaClass?.simpleName}", lastError)
            showAuthRetryExceeded = true
        }
    }

    fun onDismissAuthRetryExceeded() {
        KeystoreAuthLogger.info("KeyStoreVM", "onDismissAuthRetryExceeded → closeApp")
        showAuthRetryExceeded = false
        closeApp = true
    }

    fun openMainModuleCalled() {
        openMainModule = false
    }

    fun closeAppCalled() {
        closeApp = false
    }

}
