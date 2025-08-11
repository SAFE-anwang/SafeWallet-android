package io.horizontalsystems.bankwallet.modules.safe4.src20

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.CustomToken
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow2
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.SectionItemPosition
import io.horizontalsystems.bankwallet.ui.compose.components.SectionUniversalItem
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.body_issykBlue
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper

class SRC20ManagerFragment: BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<SRC20Module.Input>()
        val wallet = input?.wallet
        if (wallet == null) {
            navController.popBackStack(R.id.nodeListFragment, true)
            return
        }
        val viewModel by viewModels<SRC20ManagerViewModel> { SRC20Module.Factory(wallet) }
        SRC20MangerScreen(wallet, viewModel = viewModel, navController = navController)
    }
}



@Composable
fun SRC20MangerScreen(
    wallet: Wallet,
    viewModel: SRC20ManagerViewModel,
    navController: NavController
) {
    val uiState = viewModel.uiState
    val managerItems = uiState.list
    val view = LocalView.current

    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = stringResource(id = R.string.SRC20_Manger),
            navigationIcon = {
                HsBackButton(onClick = { navController.popBackStack() })
            }
        )
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            Scaffold(
                backgroundColor = ComposeAppTheme.colors.tyler,
            ) { paddingValues ->
                if (managerItems.isNullOrEmpty()) {
                    Column(Modifier.padding(paddingValues)) {
                        if (managerItems == null) {
                            ListEmptyView(
                                text = stringResource(R.string.Transactions_WaitForSync),
                                icon = R.drawable.ic_clock
                            )
                        } else {
                            ListEmptyView(
                                text = stringResource(
                                        R.string.SRC20_No_Deploy),
                                icon = R.drawable.ic_no_data
                            )
                        }
                    }
                } else {
                    val listState = rememberLazyListState()
                    LazyColumn(Modifier.padding(paddingValues), state = listState) {

                        managerList(
                            managerList = managerItems,
                            onEdit = {
                                navController.slideFromRight(
                                    R.id.src20EditFragment,
                                    SRC20Module.InputEdit(
                                        wallet, it
                                    )
                                )
                            },
                            onPromotion = {
                                navController.slideFromRight(
                                    R.id.src20PromotionFragment,
                                    SRC20Module.InputEdit(
                                        wallet, it
                                    )
                                )
                            },
                            onAdditionalIssuance = {
                                navController.slideFromRight(
                                    R.id.src20AdditionFragment,
                                    SRC20Module.InputEdit(
                                        wallet, it
                                    )
                                )
                            },
                            onDestroy = { token ->
                                val walletList: List<Wallet> = App.walletManager.activeWallets
                                var safeWallet: Wallet? = null
                                for (it in walletList) {
                                    if (it.coin.uid.contains(token.address, true)) {
                                        safeWallet = it
                                    }
                                }
                                safeWallet?.let {
                                    navController.slideFromRight(
                                        R.id.src20DestroyFragment,
                                        SRC20Module.InputEdit(
                                            it, token
                                        )
                                    )
                                }
                            },
                            onCopy = { address ->
                                TextHelper.copyText(address)
                                HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                            }
                        )
                    }
                }
            }
        }
    }

}


@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.managerList(
    managerList: List<MangerItem>,
    onEdit: (CustomToken) -> Unit,
    onPromotion: (CustomToken) -> Unit,
    onAdditionalIssuance: (CustomToken) -> Unit,
    onDestroy: (CustomToken) -> Unit,
    onCopy: (String) -> Unit
) {
    val itemsCount = managerList.size
    val singleElement = itemsCount == 1
    itemsIndexed(
        items = managerList,
        key = { _, item ->
            item.token.address
        }
    ) { index, item ->
        val position: SectionItemPosition = when {
            singleElement -> SectionItemPosition.Single
            index == 0 -> SectionItemPosition.First
            index == itemsCount - 1 -> SectionItemPosition.Last
            else -> SectionItemPosition.Middle
        }
        item.canAdditionalIssuance
        val divider = position == SectionItemPosition.Middle || position == SectionItemPosition.Last
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            SectionUniversalItem(
                borderTop = divider,
            ) {
                val clipModifier = Modifier.clip(RoundedCornerShape(12.dp))

                RowUniversal(
                    modifier = Modifier
                        .wrapContentSize()
                        .then(clipModifier)
                        .background(ComposeAppTheme.colors.lawrence)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp)
                            .alpha(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CoinImage(
                                iconUrl = item.token.logoURI,
                                placeholder = R.drawable.ic_safe_20,
                                modifier = Modifier
                                    .size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                body_bran(text = item.token.symbol)
                                Spacer(modifier = Modifier.height(4.dp))
                                body_bran(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = item.token.name
                                )
                            }

                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Row {
                            body_bran(
                                modifier = Modifier.weight(2f),
                                text = stringResource(R.string.SRC20_Info_Contract))
                            body_issykBlue(
                                modifier = Modifier.weight(5f)
                                    .clickable {
                                        onCopy.invoke(item.token.address)
                                    },
                                text = item.token.address.shorten(8))
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Row {
                            body_bran(
                                modifier = Modifier.weight(2f),
                                text = stringResource(R.string.SRC20_Info_Creator))
                            body_issykBlue(
                                modifier = Modifier.weight(5f),
                                text = item.token.creator.shorten(8))
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Row {
                            ButtonPrimaryYellow2(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(25.dp),
                                title = stringResource(R.string.SRC20_Info_Edit),
                                onClick = {
                                    onEdit.invoke(item.token)
                                }
                            )
                            Spacer(Modifier.width(5.dp))

                            ButtonPrimaryYellow2(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(25.dp),
                                title = stringResource(R.string.SRC20_Info_Promotion),
                                onClick = {
                                    onPromotion.invoke(item.token)
                                }
                            )
                            Spacer(Modifier.width(5.dp))


                            ButtonPrimaryYellow2(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(25.dp),
                                title = stringResource(R.string.SRC20_Info_Add),
                                onClick = {
                                    onAdditionalIssuance.invoke(item.token)
                                },
                                enabled = item.canAdditionalIssuance
                            )
                            Spacer(Modifier.width(5.dp))


                            ButtonPrimaryYellow2(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(25.dp),
                                title = stringResource(R.string.SRC20_Info_Destroy),
                                onClick = {
                                    onDestroy.invoke(item.token)
                                },
                                enabled = item.canDestroy
                            )
                            Spacer(Modifier.width(5.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))
    }
}