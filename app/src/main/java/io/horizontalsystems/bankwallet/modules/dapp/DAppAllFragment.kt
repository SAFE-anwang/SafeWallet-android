package io.horizontalsystems.bankwallet.modules.dapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.databinding.FragmentDappAllBinding
import io.horizontalsystems.core.findNavController

class DAppAllFragment: BaseFragment() {

    private val viewModel by navGraphViewModels<DAppViewModel>(R.id.mainFragment) { DAppModule.Factory() }

    private var _binding: FragmentDappAllBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDappAllBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun setUI() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        val adapter = DAppItemAdapter(viewModel.tempListApp, object : DAppAdapter.Listener {
            override fun onAllDApp(type: String) {

            }

            override fun onClick(dappItem: DAppItem) {
                startActivity(Intent(requireActivity(), DAppBrowseActivity::class.java).apply {
                    putExtra("url", dappItem.dlink)
                    putExtra("name", dappItem.name)
                })
            }
        })

        binding.rvItems.adapter = adapter
    }
}
