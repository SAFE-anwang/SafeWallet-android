package io.horizontalsystems.bankwallet.modules.swap.liquidity.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.navGraphViewModels
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.amount.LiquidityAmountInput
import io.horizontalsystems.bankwallet.modules.balance.ui.LiquidityItemsEmpty
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.liquidity.list.ui.LiquidityItems
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.ConfirmationDialog
import io.horizontalsystems.core.findNavController
import java.math.BigDecimal

class LiquidityListFragment : BaseFragment() {

    val mainViewModel by navGraphViewModels<LiquidityListViewModel>(R.id.listLiquidity) {
        LiquidityListModule.Factory()
    }
//    val mainViewModel: LiquidityListViewModel by viewModels { LiquidityListModule.Factory()  }
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
//                val mainViewModel: LiquidityListViewModel by viewModels { LiquidityListModule.Factory()  }
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
                            mainViewModel.tempItem = item
                            mainViewModel.tempIndex = index
                            findNavController().slideFromRight(R.id.removeLiquidity)
//                            confirm(index, item, mainViewModel)
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
//                    mainViewModel.reset()
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
            val tabs = viewModel.tabs
            val selectedTab = viewModel.selectedTab
            val tabItems = tabs.map {
                TabItem(stringResource(id = it.titleResId), it == selectedTab, it)
            }
            Tabs(tabItems, onClick = {
                viewModel.onSelect(it)
            })

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