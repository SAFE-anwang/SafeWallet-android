package io.horizontalsystems.bankwallet.modules.dapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.databinding.FragmentDappBinding
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import java.util.HashMap

class DAppFragment: BaseFragment() {

    private val viewModel by navGraphViewModels<DAppViewModel>(R.id.mainFragment) { DAppModule.Factory() }
//    private lateinit var recommendAdapter: DAppAdapter
    private lateinit var classifyAdapter: DAppAdapter

    private var _binding: FragmentDappBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDappBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUI()
    }

    private fun setUI() {
        binding.tabsCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this)
        )
//        viewModel.filterTypesLiveData.observe(viewLifecycleOwner, Observer {
            binding.tabsCompose.setContent {
                ComposeAppTheme {
                    /*FilterTypeTabs(
                        it,
                        { viewModel.setFilterDAppType(it) },
                        {  })*/
                    DAppScreen(viewModel = viewModel, navController = findNavController())
                }
            }
//        })
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
        binding.dappSearch.setOnClickListener {
            findNavController().slideFromRight(R.id.dappFragmentSearch)
        }
        val listener = object : DAppAdapter.Listener {
            override fun onAllDApp(type: String) {
                findNavController().slideFromRight(R.id.dappFragmentAll)
            }

            override fun onClick(dappItem: DAppItem) {
                /*findNavController().slideFromBottom(
                    R.id.mainFragment_to_wcSendEthereumTransactionRequestFragment
                )*/
                val bundle = Bundle()
                bundle.putString("url", dappItem.dlink)
                bundle.putString("name", dappItem.name)
                findNavController().slideFromRight(R.id.dappBrowseFragment, bundle)
                /*startActivity(Intent(requireActivity(), DAppBrowseActivity::class.java).apply {
                    putExtra("url", dappItem.dlink)
                    putExtra("name", dappItem.name)
                })*/
            }

        }
//        recommendAdapter = DAppAdapter(viewModel, listener)
        classifyAdapter = DAppAdapter(viewModel, listener)
        setAdapter()

        /*viewModel.recommendsAppList.observe(viewLifecycleOwner) {
            recommendAdapter.items = it as HashMap<String, List<DAppItem>>
            recommendAdapter.notifyDataSetChanged()

        }*/
        viewModel.dAppList.observe(viewLifecycleOwner) {
            classifyAdapter.items = it as HashMap<String, List<DAppItem>>
            classifyAdapter.notifyDataSetChanged()
        }
        viewModel.viewState.observe(viewLifecycleOwner) {
            if (it != ViewState.Loading) {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun setAdapter() {
        val concatAdapter = ConcatAdapter(/*recommendAdapter,*/ classifyAdapter)
        binding.rvItems.adapter = concatAdapter
    }

}




@Composable
private fun DAppScreen(viewModel: DAppViewModel, navController: NavController) {
    val dAppItems by viewModel.dAppList.observeAsState()
    val viewState by viewModel.viewState.observeAsState()
    val filterTypes by viewModel.filterTypesLiveData.observeAsState()
    val syncing by viewModel.syncingLiveData.observeAsState(false)

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            filterTypes?.let { filterTypes ->
                FilterTypeTabs(
                    filterTypes,
                    { viewModel.setFilterDAppType(it) },
                    {  }
                )
            }
        }
    }
}


@Composable
private fun FilterTypeTabs(
    filterTypes: List<Filter<FilterDAppType>>,
    onDAppTypeClick: (FilterDAppType) -> Unit,
    scrollToTopAfterUpdate: () -> Unit
) {
    val tabItems = filterTypes.map {
        TabItem(stringResource(it.item.title), it.selected, it.item)
    }

    Tabs(tabItems) { dAppType ->
        onDAppTypeClick.invoke(dAppType)
        scrollToTopAfterUpdate.invoke()
    }
}