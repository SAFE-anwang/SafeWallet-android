package io.horizontalsystems.bankwallet.modules.restore.restoreotherwallet.privatekey

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.IconButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.core.utils.Utils
import io.horizontalsystems.bankwallet.databinding.FragmentRestorePrivateKeyImportBinding
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.restore.restoreotherwallet.WalletType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.KeyboardHelper

class PrivateKeyImportFragment: BaseFragment() {

    private var _binding: FragmentRestorePrivateKeyImportBinding? = null
    private val binding get() = _binding!!

    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable) {
            if (s.isNotEmpty()) {

            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            isUsingNativeKeyboard()
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRestorePrivateKeyImportBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbarCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this)
        )
        binding.toolbarCompose.setContent {
            val qrScannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val scannedText = result.data?.getStringExtra(ModuleField.SCAN_ADDRESS) ?: ""

                    binding.wordsInput.setText(scannedText)
                }
            }
            ComposeAppTheme {
                AppBar(
                    title = TranslatableString.ResString(R.string.Restore_Import_Recovery_Phrase),
                    navigationIcon = {
                        IconButton(onClick = { findNavController().popBackStack() }) {
                            Image(painter = painterResource(id = R.drawable.ic_back),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .size(24.dp)
                            )
                        }

                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Restore_Import_Private_Key),
                            icon = R.drawable.ic_qr_scan_24px,
                            onClick = {
                                qrScannerLauncher.launch(QRScannerActivity.getScanQrIntent(requireContext()))
                            }
                        )
                    )
                )
            }
        }
        binding.importButton.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this)
        )
        binding.importButton.setContent {
            ComposeAppTheme {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    title = stringResource(R.string.Restore_Import_Wallet),
                    onClick = {

                    }
                )
            }
        }

        binding.wordsInput.addTextChangedListener(textWatcher)
        KeyboardHelper.showKeyboardDelayed(requireActivity(), binding.wordsInput, 200)
    }

    private fun isUsingNativeKeyboard(): Boolean {
        if (Utils.isUsingCustomKeyboard(requireContext()) && !CoreApp.thirdKeyboardStorage.isThirdPartyKeyboardAllowed) {
            showCustomKeyboardAlert()
            return false
        }

        return true
    }
}