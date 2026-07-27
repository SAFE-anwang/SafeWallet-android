package io.horizontalsystems.bankwallet.modules.keystore

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.KeystoreAuthLogger
import io.horizontalsystems.core.IKeyStoreManager
import io.horizontalsystems.core.security.KeyStoreValidationError

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

        // Immediately validate KeyStore after authentication to break potential loop
        try {
            KeystoreAuthLogger.info("KeyStoreVM", "Calling keyStoreManager.validateKeyStore() after auth success...")
            keyStoreManager.validateKeyStore()
            KeystoreAuthLogger.info("KeyStoreVM", "validateKeyStore() PASSED after auth success → openMainModule")
            openMainModule = true
        } catch (e: KeyStoreValidationError.UserNotAuthenticated) {
            // Still not authenticated despite BiometricPrompt success → show error
            KeystoreAuthLogger.error("KeyStoreVM", "validateKeyStore() STILL UserNotAuthenticated after BiometricPrompt success! → showAuthRetryExceeded", e)
            showAuthRetryExceeded = true
        } catch (e: Exception) {
            KeystoreAuthLogger.error("KeyStoreVM", "validateKeyStore() failed after BiometricPrompt success | ${e.javaClass.simpleName}", e)
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
