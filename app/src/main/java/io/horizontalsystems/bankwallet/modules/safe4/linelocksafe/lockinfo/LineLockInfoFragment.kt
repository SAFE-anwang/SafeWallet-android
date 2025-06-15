package io.horizontalsystems.bankwallet.modules.safe4.linelocksafe.lockinfo

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.modules.safe4.linelocksafe.LineLockSafeModule
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourModule
import io.horizontalsystems.bankwallet.modules.transactions.TransactionCell
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderStick
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.SectionItemPosition
import io.horizontalsystems.bankwallet.ui.compose.components.SectionUniversalItem
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.body_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.sectionItemBorder

class LineLockInfoFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<SafeFourModule.LineLockInput>()
        val wallet = input?.wallet
        if (wallet == null) {
            Toast.makeText(App.instance, "Wallet is Null", Toast.LENGTH_SHORT).show()
            navController.popBackStack(R.id.sendSafe4LockFragment, true)
            return
        }
        val viewModel by viewModels<LineLockInfoViewModel> { LineLockSafeModule.LinLockInfoFactory(wallet) }

        LineLockInfoScreen(
            viewModel,
            navController
        )
    }
}

@Composable
fun LineLockInfoScreen(
    viewModel: LineLockInfoViewModel,
    navController: NavController
) {
    val uiState = viewModel.uiState

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(id = R.string.Safe4_Lock_Info),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                }
            )
        }
    ) { paddingValues ->
        val transactionItems = uiState.transactions
        if (transactionItems.isNullOrEmpty()) {
            Column(Modifier.padding(paddingValues)) {
                if (transactionItems == null) {
                    ListEmptyView(
                        text = stringResource(R.string.Transactions_WaitForSync),
                        icon = R.drawable.ic_clock
                    )
                } else {
                    ListEmptyView(
                        text = stringResource(R.string.SAFE4_Line_Lock_No_Data),
                        icon = R.drawable.ic_outgoingraw
                    )
                }
            }
        } else {
            val listState = rememberLazyListState()
            LazyColumn(Modifier.padding(paddingValues), state = listState) {
                val itemsCount = transactionItems.size
                itemsIndexed(
                    items = transactionItems,
                    key = { index, item ->
                        index
                    }
                ) { index, item ->
                    val position: SectionItemPosition = when {
                        index == 0 -> SectionItemPosition.First
                        index == itemsCount - 1 -> SectionItemPosition.Last
                        else -> SectionItemPosition.Middle
                    }
                    val divider = position == SectionItemPosition.Middle || position == SectionItemPosition.Last
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SectionUniversalItem(
                            borderTop = divider,
                        ) {
                            val clipModifier = when (position) {
                                SectionItemPosition.First -> {
                                    Modifier.clip(
                                        RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp
                                        )
                                    )
                                }

                                SectionItemPosition.Last -> {
                                    Modifier.clip(
                                        RoundedCornerShape(
                                            bottomStart = 12.dp,
                                            bottomEnd = 12.dp
                                        )
                                    )
                                }

                                SectionItemPosition.Single -> {
                                    Modifier.clip(RoundedCornerShape(12.dp))
                                }

                                else -> Modifier
                            }

                            val borderModifier = if (position != SectionItemPosition.Single) {
                                Modifier.sectionItemBorder(
                                    1.dp,
                                    ComposeAppTheme.colors.steel20,
                                    12.dp,
                                    position
                                )
                            } else {
                                Modifier.border(
                                    1.dp,
                                    ComposeAppTheme.colors.steel20,
                                    RoundedCornerShape(12.dp)
                                )
                            }

                            RowUniversal(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(clipModifier)
                                    .then(borderModifier)
                                    .background(ComposeAppTheme.colors.lawrence)
                            ) {
                                Column (
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .wrapContentHeight(),
                                ) {
                                    Row {
                                        Image(
                                            painter = painterResource(R.drawable.ic_lock_20),
                                            contentDescription = null
                                        )
                                        body_bran(text = item.value)
                                        Spacer(Modifier.weight(1f))
                                        body_jacob(text = stringResource(R.string.SAFE4_Line_Lock_Day, item.days))
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    body_bran(text = item.address)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }

            }
        }
    }
}
