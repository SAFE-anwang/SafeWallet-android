package io.horizontalsystems.bankwallet.modules.swap.liquidity.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.liquidity.ui.LiquidityItems
import io.horizontalsystems.bankwallet.modules.swap.liquidity.ui.LiquidityItemsEmpty
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.Token

class LiquidityFragment(): BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                LiquidityListScreen(findNavController(), arguments?.getParcelable(LiquidityModule.TOKEN_KEY)!!)
            }
        }
    }

}


@Composable
fun LiquidityListScreen(navController: NavController, token: Token) {
    val viewModel = viewModel<LiquidityViewModel>(factory = LiquidityModule.Factory(token))
    Log.e("longwen", "token=$token")
    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.liquidity_title),
                navigationIcon = {
                    HsIconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = null,
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.ManageCoins_AddToken),
                        icon = R.drawable.ic_add_yellow,
                        onClick = {
                            navController.slideFromBottom(
                                R.id.swapFragment,
                                SwapMainModule.prepareParams(token, true)
                            )
                        }
                    )
                )
            )
            val uiState = viewModel.uiState
//            Liquidity(viewModel = viewModel, uiState = uiState)

            Crossfade(uiState.viewState) { viewState ->
                when (viewState) {
                    ViewState.Success -> {
                        val balanceViewItems = uiState.liquidityViewItems

                        if (balanceViewItems.isNotEmpty()) {
                            LiquidityItems(
                                balanceViewItems,
                                viewModel,
                                navController,
                                uiState
                            )
                        } else {
                            LiquidityItemsEmpty(navController)
                        }
                    }
                    ViewState.Loading,
                    is ViewState.Error -> {
                    }
                }
            }
        }
    }
}

@Composable
fun Liquidity(
    viewModel: LiquidityViewModel,
    uiState: LiquidityViewModel.LiquidityUiState
) {
    val listState = rememberSaveable(
//        accountId,
//        sortType,
        saver = LazyListState.Saver
    ) {
        LazyListState()
    }

    HSSwipeRefresh(
        state = rememberSwipeRefreshState(uiState.isRefreshing),
        onRefresh = {
            viewModel.onRefresh()
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = 8.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

        }
    }
}
