package io.horizontalsystems.bankwallet.modules.swap.liquidity.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.balance.ui.LiquidityItemsEmpty
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.swap.liquidity.list.ui.LiquidityItems
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.ConfirmationDialog
import io.horizontalsystems.core.findNavController

class LiquidityListFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            try {
                val mainViewModel: LiquidityListViewModel by viewModels { LiquidityListModule.Factory()  }
                mainViewModel.removeErrorMessage.observe(viewLifecycleOwner, Observer {
                    it?.let {
                        Toast.makeText(App.instance, it, Toast.LENGTH_SHORT).show()
                    }
                })
                mainViewModel.removeSuccessMessage.observe(viewLifecycleOwner, Observer {
                    it?.let {
                        Toast.makeText(App.instance, it, Toast.LENGTH_SHORT).show()
                    }
                    findNavController().popBackStack()
                })
                setContent {
                    ComposeAppTheme {
                        LiquidityForAccount(
                            findNavController(),
                            mainViewModel
                        ) { index, item ->
                            confirm(index, item, mainViewModel)
                        }
                    }
                }
            } catch (t: Throwable) {
                Toast.makeText(
                    App.instance, t.message ?: t.javaClass.simpleName, Toast.LENGTH_SHORT
                ).show()
                findNavController().popBackStack()
            }
        }
    }

    private fun confirm(index: Int, item: LiquidityViewItem, mainViewModel: LiquidityListViewModel) {
        ConfirmationDialog.show(
            title = getString(R.string.liquidity_remove_title),
            warningText = getString(R.string.Liquidity_Remove_Confirm),
            actionButtonTitle = getString(R.string.Liquidity_Remove_Confirm_OK),
            transparentButtonTitle = getString(R.string.Liquidity_Remove_Confirm_Cancel),
            fragmentManager = childFragmentManager,
            listener = object : ConfirmationDialog.Listener {
                override fun onActionButtonClick() {
                    mainViewModel.removeLiquidity(index, item.walletA, item.addressA, item.walletB, item.addressB)
                }

                override fun onTransparentButtonClick() {

                }

                override fun onCancelButtonClick() {

                }
            }
        )
    }

}


@Composable
fun LiquidityForAccount(
    navController: NavController,
    viewModel: LiquidityListViewModel,
    removeCallback: (Int, LiquidityViewItem) -> Unit
) {
    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                title = stringResource(R.string.liquidity_title),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = {
                            navController.popBackStack()
                        }
                    )
                )
            )

            val uiState = viewModel.uiState

            Crossfade(uiState.viewState) { viewState ->
                when (viewState) {
                    ViewState.Success -> {
                        val liquidityViewItems = uiState.liquidityViewItems

                        if (liquidityViewItems.isNotEmpty()) {
                            LiquidityItems(
                                liquidityViewItems,
                                viewModel,
                                navController,
                                uiState,
                                removeCallback
                            )
                        } else {
                            LiquidityItemsEmpty(navController)
                        }
                    }

                    ViewState.Loading-> {
                        Loading()
                    }
                    is ViewState.Error -> {
                    }
                }
            }
        }
    }
}