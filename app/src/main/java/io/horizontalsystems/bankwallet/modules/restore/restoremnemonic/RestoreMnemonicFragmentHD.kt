package io.horizontalsystems.bankwallet.modules.restore.restoremnemonic

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.IconButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.core.utils.Utils
import io.horizontalsystems.bankwallet.databinding.FragmentRestoreMnemonicHdBinding
import io.horizontalsystems.bankwallet.modules.restore.restoreblockchains.RestoreBlockchainsFragment.Companion.ACCOUNT_TYPE_KEY
import io.horizontalsystems.bankwallet.modules.restore.restoreblockchains.RestoreBlockchainsFragment.Companion.PURPOSE_TYPE_KEY
import io.horizontalsystems.bankwallet.modules.restore.restoreotherwallet.WalletType
import io.horizontalsystems.bankwallet.modules.restore.restoreotherwallet.phrase.SelectWalletViewModel
import io.horizontalsystems.bankwallet.modules.restore.restoreotherwallet.phrase.WalletInfo
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationResult
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.helpers.KeyboardHelper
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic

class RestoreMnemonicFragmentHD : BaseFragment() {
    private val viewModel by viewModels<RestoreMnemonicViewModel> { RestoreMnemonicModule.Factory() }

    private val selectWalletViewModel = SelectWalletViewModel()
    private var purpose = HDWallet.Purpose.BIP49

    val bip32Path = listOf<String>("m/49'/0'/0'", "m/44'/0'/0'", "m/84'/0'/0'")

    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable) {
            if (s.isNotEmpty()) {
                viewModel.onTextChange(s.toString(), binding.wordsInput.selectionStart)
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            isUsingNativeKeyboard()
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }
    }

    private var _binding: FragmentRestoreMnemonicHdBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRestoreMnemonicHdBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // IM Token的钱包，不需要输入密码
        val walletType = arguments?.getParcelable("walletType") as? WalletType
        binding.toolbarCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this)
        )
        val menus = if (walletType !is WalletType.SafeGem) {
            listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Next),
                    onClick = {
                        viewModel.onProceed()
                    }
                )
            )
        } else {
            listOf()
        }
        binding.toolbarCompose.setContent {
            ComposeAppTheme {
                AppBar(
                    title = TranslatableString.ResString(R.string.Restore_Enter_Key_Title_HD),
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
                    menuItems = menus
                )
            }
        }


        when (walletType) {
            is WalletType.TokenPocket,
            is WalletType.ImToken,
            is WalletType.Bither -> {
                binding.passphraseToggle.visibility = View.GONE
                binding.passphrase.visibility = View.GONE
                binding.passphraseDescription.visibility = View.GONE
                binding.walletName.visibility = View.GONE
                binding.inputWalleName.visibility = View.GONE
                binding.walletPath.visibility = View.GONE
                binding.pathSelect.visibility = View.GONE
            }
            is WalletType.SafeGem -> {
                binding.llNotImplemented.visibility = View.VISIBLE
            }
        }

        binding.walletName.setOnClickListener {
            findNavController().navigate(R.id.restoreSelectWalletNameFragment)
        }
        binding.inputWalleName.onTextChange { prevText, newText ->
            if (prevText != newText) updateWalletPath(newText)
        }

        // HD钱包，有输入钱包名称功能
        walletType?.let {
            if (it is WalletType.HD) {
                binding.walletName.visibility = View.VISIBLE
                binding.inputWalleName.visibility = View.VISIBLE
                binding.pathSelect.visibility = View.VISIBLE
                binding.pathDescription.visibility = View.VISIBLE
                binding.inputWalleName.setEditable(false)

                initBip32Path(bip32Path)
            }
            if (it is WalletType.Bither || it is WalletType.SafeGem) {
                purpose = HDWallet.Purpose.BIP44
            }
        }

        bindListeners()
        observeEvents()
        if (walletType !is WalletType.SafeGem) {
            KeyboardHelper.showKeyboardDelayed(requireActivity(), binding.wordsInput, 200)
        }
    }

    override fun onResume() {
        super.onResume()
        getNavigationResult("walletName")?.let {
            val name = it.getString("name")
            binding.inputWalleName.setText(name)
//            binding.walletName.setText(name)
            updateWalletPath(name)
        }
    }

    private fun observeEvents() {
        viewModel.proceedLiveEvent.observe(viewLifecycleOwner, Observer { accountType ->
            hideKeyboard()
            findNavController().slideFromRight(
                R.id.restoreSelectCoinsFragment,
                bundleOf(ACCOUNT_TYPE_KEY to accountType, PURPOSE_TYPE_KEY to purpose.value)
            )
        })

        viewModel.invalidRangesLiveData.observe(viewLifecycleOwner, Observer { invalidRanges ->
            binding.wordsInput.removeTextChangedListener(textWatcher)

            val cursor = binding.wordsInput.selectionStart
            val spannableString = SpannableString(binding.wordsInput.text.toString())

            invalidRanges.forEach { range ->
                val spannableColorSpan =
                    ForegroundColorSpan(requireContext().getColor(R.color.lucian))
                if (range.last < binding.wordsInput.text.length) {
                    spannableString.setSpan(
                        spannableColorSpan,
                        range.first,
                        range.last + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            binding.wordsInput.setText(spannableString)
            binding.wordsInput.setSelection(cursor)
            binding.wordsInput.addTextChangedListener(textWatcher)
        })

        viewModel.errorLiveData.observe(viewLifecycleOwner, Observer {
            val errorMessage = when (it) {
                is RestoreMnemonicService.ValidationError.InvalidWordCountException -> getString(
                    R.string.Restore_Error_MnemonicWordCount,
                    it.count
                )
                is Mnemonic.ChecksumException -> getString(R.string.Restore_InvalidChecksum)
                else -> getString(R.string.Restore_ValidationFailed)
            }
            HudHelper.showErrorMessage(this.requireView(), errorMessage)
        })

        viewModel.inputsVisibleLiveData.observe(viewLifecycleOwner) {
            binding.passphraseToggle.setChecked(it)
            binding.passphrase.isVisible = it
            binding.passphraseDescription.isVisible = it
        }

        viewModel.passphraseCautionLiveData.observe(viewLifecycleOwner) {
            binding.passphrase.setError(it)
        }

        viewModel.clearInputsLiveEvent.observe(viewLifecycleOwner) {
            binding.passphrase.setText(null)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindListeners() {
        binding.wordsInput.addTextChangedListener(textWatcher)

        //fixes scrolling in EditText when it's inside NestedScrollView
        binding.wordsInput.setOnTouchListener { v, event ->
            if (binding.wordsInput.hasFocus()) {
                v.parent.requestDisallowInterceptTouchEvent(true)
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_SCROLL -> {
                        v.parent.requestDisallowInterceptTouchEvent(false)
                        return@setOnTouchListener true
                    }
                }
            }
            return@setOnTouchListener false
        }

        binding.passphraseToggle.setOnCheckedChangeListenerSingle {
            viewModel.onTogglePassphrase(it)
        }

        binding.passphrase.onTextChange { old, new ->
            if (viewModel.validatePassphrase(new)) {
                viewModel.onChangePassphrase(new ?: "")
            } else {
                binding.passphrase.revertText(old)
            }
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
        if (name.isNullOrBlank()) {
            viewModel.onTogglePassphrase(false)
            binding.passphraseToggle.visibility = View.VISIBLE
            setWalletPath(null)
            return
        }
        selectWalletViewModel.getWalletForName(name)?.let {
            setWalletPath(it)
        }

    }

    private fun setWalletPath(walletInfo: WalletInfo?) {
        if (walletInfo == null) {
//            binding.customPath.visibility = View.GONE
            initBip32Path(bip32Path)
            return
        }
        initBip32Path(walletInfo.bip32path)
        /*if (walletInfo.custompath) {
            binding.customPath.visibility = View.VISIBLE
        }*/
        // 是否需要密码
        // 必要
        if (walletInfo.needpassword == 1) {
//            viewModel.onTogglePassphrase(true)
            binding.passphraseToggle.visibility = View.GONE
        } else { // 可有可无
            viewModel.onTogglePassphrase(false)
            binding.passphraseToggle.visibility = View.VISIBLE
        }
    }

    private fun initBip32Path(pathList: List<String>) {
        binding.inputWalletPath.adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_expandable_list_item_1, pathList)
        binding.inputWalletPath.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val path = pathList[position]
                val splits = path.split("/")
                purpose = when(splits[1]) {
                    "44'" -> HDWallet.Purpose.BIP44
                    "49'" -> HDWallet.Purpose.BIP49
                    "84'" -> HDWallet.Purpose.BIP84
                    else -> HDWallet.Purpose.BIP44
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
    }
}
