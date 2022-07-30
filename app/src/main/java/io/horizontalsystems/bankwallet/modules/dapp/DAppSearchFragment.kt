package io.horizontalsystems.bankwallet.modules.dapp

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.databinding.FragmentDappSearchBinding
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsModule
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.KeyboardHelper

class DAppSearchFragment: BaseFragment() {

    private val viewModel by viewModels<DAppSearchViewModel> { DAppModule.FactorySearch() }
    private var _binding: FragmentDappSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: DAppItemAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDappSearchBinding.inflate(inflater, container, false)
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
        adapter = DAppItemAdapter(ArrayList(), object : DAppAdapter.Listener {
            override fun onAllDApp(type: String) {

            }

            override fun onClick(dappItem: DAppItem) {
                val bundle = Bundle()
                bundle.putString("url", dappItem.dlink)
                bundle.putString("name", dappItem.name)
                findNavController().slideFromRight(R.id.dappBrowseFragment, bundle)
            }

        })
        binding.search.requestFocus()
        binding.search.post {
            context?.let {
                KeyboardHelper.showKeyboard(it, binding.search)
            }
        }

        binding.rvItems.adapter = adapter
        viewModel.searchResultList.observe(viewLifecycleOwner) {
            adapter.updateData(it)
        }
        viewModel.viewState.observe(viewLifecycleOwner) {
            context?.let {
                KeyboardHelper.hideKeyboard(it, binding.search)
            }
        }
        binding.search.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.search(binding.search.text.toString())
            }
            true
        }

        binding.noResultView.setContent {
            ComposeAppTheme {
                EmptyScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun EmptyScreen(viewModel: DAppSearchViewModel) {
    val dAppItems by viewModel.searchResultList.observeAsState()
    val viewState by viewModel.viewState.observeAsState()
    val syncing by viewModel.syncingLiveData.observeAsState(false)

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            Crossfade(viewState) { viewState ->
                when (viewState) {
                    ViewState.Success -> {
                        dAppItems?.let { items ->
                            if (items.isEmpty()) {
                                if (syncing) {
                                    ListEmptyView(
                                        text = stringResource(R.string.DApp_Searching),
                                        icon = R.drawable.ic_clock
                                    )
                                } else {
                                    ListEmptyView(
                                        text = stringResource(R.string.DApp_Search_no_data),
                                        icon = R.drawable.ic_image_empty
                                    )
                                }
                            }
                        }
                    }
                    is ViewState.Error -> {
                        ListErrorView(
                            errorText = stringResource(R.string.DApp_Search_error),
                            onClick = viewModel::reload
                        )
                    }
                }
            }
        }
    }
}