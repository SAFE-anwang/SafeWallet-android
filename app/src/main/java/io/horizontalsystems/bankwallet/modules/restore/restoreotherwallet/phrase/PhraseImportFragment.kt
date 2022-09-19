package io.horizontalsystems.bankwallet.modules.restore.restoreotherwallet.phrase

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.core.utils.Utils
import io.horizontalsystems.bankwallet.databinding.FragmentRestorePhraseImportBinding
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.restore.restoreotherwallet.LanguageType
import io.horizontalsystems.bankwallet.modules.restore.restoreotherwallet.WalletType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationResult
import io.horizontalsystems.core.helpers.KeyboardHelper
import io.horizontalsystems.core.setNavigationResult
import io.horizontalsystems.hdwalletkit.HDWallet

class PhraseImportFragment: BaseFragment() {

    private var _binding: FragmentRestorePhraseImportBinding? = null
    private val binding get() = _binding!!
    private var currentLanguage: LanguageType = LanguageType.English
    private var walletType: WalletType? = null

    private val selectWalletViewModel = SelectWalletViewModel()

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
        _binding = FragmentRestorePhraseImportBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        walletType = arguments?.getParcelable("walletType")

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
                            title = TranslatableString.ResString(R.string.Restore_Import_Recovery_Phrase),
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
        binding.language.onLanguageChange {
            currentLanguage = it
        }

        binding.wordsInput.addTextChangedListener(textWatcher)
        binding.walletName.setOnClickListener {
            findNavController().navigate(R.id.restoreSelectWalletNameFragment)
        }
        binding.inputWalleName.onTextChange { prevText, newText ->
            updateWalletPath(newText)
        }
        KeyboardHelper.showKeyboardDelayed(requireActivity(), binding.wordsInput, 200)

        // HD钱包，有输入钱包名称功能
        walletType?.let {
            if (it is WalletType.HD) {
                binding.walletName.visibility = View.VISIBLE
                binding.inputWalleName.visibility = View.VISIBLE
                binding.pathSelect.visibility = View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        getNavigationResult("walletName")?.let {
            val name = it.getString("name")
            updateWalletPath(name)
        }
    }

    private fun isUsingNativeKeyboard(): Boolean {
        if (Utils.isUsingCustomKeyboard(requireContext()) && !CoreApp.thirdKeyboardStorage.isThirdPartyKeyboardAllowed) {
            showCustomKeyboardAlert()
            return false
        }
        return true
    }

    private fun updateWalletPath(name: String?) {
        binding.inputWalleName.setText(name)
        if (name.isNullOrBlank()) {
            setWalletPath(null)
            return
        }
        selectWalletViewModel.getWalletForName(name)?.let {
            setWalletPath(it)
        }

    }

    private fun setWalletPath(walletInfo: WalletInfo?) {
        if (walletInfo == null) {
            binding.customPath.visibility = View.GONE
            binding.inputWalletPath.adapter = null
            return
        }
        binding.inputWalletPath.adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_expandable_list_item_1, walletInfo.bip32path)
        binding.inputWalletPath.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                Log.e("longwen", "select path: ${walletInfo.bip32path[position]}")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        if (walletInfo.custompath) {
            binding.customPath.visibility = View.VISIBLE
        }
    }
}