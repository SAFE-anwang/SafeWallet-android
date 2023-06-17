package io.horizontalsystems.bankwallet.modules.restore.restoreotherwallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.databinding.FragmentSelectWalletTypeBinding
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.core.findNavController

class SelectWalletTypeFragment: BaseFragment() {

    private var _binding: FragmentSelectWalletTypeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectWalletTypeBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        val popUpToInclusiveId =
            arguments?.getInt(ManageAccountsModule.popOffOnSuccessKey, R.id.manageAccountsFragment) ?: R.id.manageAccountsFragment
        val inclusive =
            arguments?.getBoolean(ManageAccountsModule.popOffInclusiveKey) ?: true
        val adapter = SelectWalletTypeAdapter(getWalletTypeList()) {
            val bundle = Bundle()
            bundle.putParcelable("walletType", it)
            bundle.putInt(ManageAccountsModule.popOffOnSuccessKey, popUpToInclusiveId)
            bundle.putBoolean(ManageAccountsModule.popOffInclusiveKey, inclusive)
            when(it) {
                WalletType.SafeWallet -> {
                    findNavController().slideFromRight(R.id.restoreMnemonicFragment, bundle)
                }
                WalletType.TokenPocket,
                WalletType.SafeGem,
                WalletType.Bither,
                WalletType.ImToken,
                WalletType.HD -> {
                    findNavController().slideFromRight(R.id.restoreMnemonicFragmentHd, bundle)
                }
                else -> {
//                    findNavController().slideFromRight(R.id.restorePhraseImportFragment, bundle)
                }
            }
        }
        val gridLayoutManager = LinearLayoutManager(context)
        binding.rvWallet.layoutManager = gridLayoutManager
        binding.rvWallet.adapter = adapter

    }

    private fun getWalletTypeList(): List<WalletType> {
        val list = mutableListOf<WalletType>()
        list.add(WalletType.HD)
        list.add(WalletType.SafeWallet)
        list.add(WalletType.ImToken)
        list.add(WalletType.Bither)
        list.add(WalletType.SafeGem)
        list.add(WalletType.TokenPocket)
        return list
    }

}