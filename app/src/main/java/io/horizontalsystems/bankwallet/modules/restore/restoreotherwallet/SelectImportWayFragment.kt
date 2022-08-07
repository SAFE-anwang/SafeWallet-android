package io.horizontalsystems.bankwallet.modules.restore.restoreotherwallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.databinding.FragmentSelectImportTypeBinding
import io.horizontalsystems.core.findNavController

class SelectImportWayFragment: BaseFragment() {

    private var _binding: FragmentSelectImportTypeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectImportTypeBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.llPrivateKey.setOnClickListener {
            findNavController().slideFromRight(R.id.restorePrivateKeyImportFragment, arguments)
        }
        binding.llPhrase.setOnClickListener {
            findNavController().slideFromRight(R.id.restorePhraseImportFragment, arguments)
        }
    }

}