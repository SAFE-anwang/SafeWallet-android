package io.horizontalsystems.bankwallet.modules.restore.restoreotherwallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.databinding.FragmentSelectWalletTypeBinding
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
        val adapter = SelectWalletTypeAdapter(getWalletTypeList()) {
            val bundle = Bundle()
            bundle.putParcelable("walletType", it)
            when(it) {
                WalletType.SafeWallet -> {
                    findNavController().slideFromRight(R.id.restoreMnemonicFragment, bundle)
                }
                WalletType.ImToken,
                WalletType.HD -> {
                    findNavController().slideFromRight(R.id.restoreMnemonicFragmentHd, bundle)
                }
                else -> {
                    findNavController().slideFromRight(R.id.restorePhraseImportFragment, bundle)
                }
            }
        }
        val gridLayoutManager = LinearLayoutManager(context)
        binding.rvWallet.layoutManager = gridLayoutManager
        binding.rvWallet.adapter = adapter

    }

    private fun getWalletTypeList(): List<WalletType> {
        val list = mutableListOf<WalletType>()
        list.add(WalletType.SafeWallet)
        list.add(WalletType.SafeGem)
        list.add(WalletType.ImToken)
        list.add(WalletType.BitPie)
        list.add(WalletType.TokenPocket)
        list.add(WalletType.HD)
        return list
    }

}