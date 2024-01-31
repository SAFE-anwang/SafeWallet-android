package io.horizontalsystems.bankwallet.modules.swap.liquidity.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
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
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.balance.ui.LiquidityItemsEmpty
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.swap.liquidity.list.ui.LiquidityCardSwipable
import io.horizontalsystems.bankwallet.modules.swap.liquidity.list.ui.LiquidityItems
import io.horizontalsystems.bankwallet.modules.swap.liquidity.list.ui.RemoveLiquidityCard
import io.horizontalsystems.bankwallet.modules.swap.ui.SuggestionsBar
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Keyboard
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.compose.observeKeyboardState
import io.horizontalsystems.bankwallet.ui.extensions.ConfirmationDialog
import io.horizontalsystems.core.findNavController
import java.math.BigDecimal

class RemoveLiquidityFragment : BaseFragment() {

    val mainViewModel by navGraphViewModels<LiquidityListViewModel>(R.id.listLiquidity) {
        LiquidityListModule.Factory()
    }

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
                   // findNavController().popBackStack()
                    findNavController().popBackStack(R.id.listLiquidity, true)
                })
                setContent {
                    ComposeAppTheme {
                        RemoveLiquidityForAccount(
                            findNavController(),
                            mainViewModel
                        ) { index, item ->
                            if (mainViewModel.amountCaution == null) {
                                confirm(index, item, mainViewModel)
                            }
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

    override fun onDestroy() {
        super.onDestroy()
        mainViewModel.reset()
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
fun RemoveLiquidityForAccount(
    navController: NavController,
    viewModel: LiquidityListViewModel,
    removeCallback: (Int, LiquidityViewItem) -> Unit
) {


    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                title = stringResource(R.string.liquidity_remove_title),
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
            val amountCaution = uiState.amountCaution

            RemoveLiquidityCard(
                viewItem = viewModel.tempItem!!
            )

            Spacer(modifier = Modifier.padding(top = 8.dp))

            /*LiquidityAmountInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    focusRequester = focusRequester,
                    availableBalance = BigDecimal(viewModel.tempItem!!.liquidity),
                    caution = amountCaution,
                    coinCode = "",
                    coinDecimal = 18,
                    fiatDecimal = 2,
                    onValueChange = {
                        viewModel.onEnterAmount(it, BigDecimal(viewModel.tempItem!!.liquidity))
                    }
            )*/

            Spacer(modifier = Modifier.width(8.dp))

            PercentBar(modifier = Modifier.align(Alignment.Start), select = uiState.selectPercent) {
                viewModel.setRemovePercent(it)
            }
            Row(
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                ButtonPrimaryYellow(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.liquidity_remove_title),
                        onClick = {
                            removeCallback.invoke(viewModel.tempIndex!!, viewModel.tempItem!!)
                        },
                        enabled = true
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            VSpacer(32.dp)
        }
    }
}


@Composable
private fun PercentBar(
        modifier: Modifier = Modifier,
        select: Int = 25,
        percents: List<Int> = listOf(25, 50, 75, 100),
        onClick: (Int) -> Unit
) {
    Box(modifier = modifier) {
        BoxTyler44(borderTop = true) {
            Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
            ) {
                percents.forEach { percent ->
                    val buttonColors = if (percent == select) {
                        SecondaryButtonDefaults.buttonColors(backgroundColor = ComposeAppTheme.colors.yellowD)
                    } else {
                        SecondaryButtonDefaults.buttonColors()
                    }
                    ButtonSecondary(
                            onClick = { onClick.invoke(percent) },
                            buttonColors = buttonColors
                    ) {
                        subhead1_leah(text = "$percent%")
                    }
                }
            }
        }
    }
}