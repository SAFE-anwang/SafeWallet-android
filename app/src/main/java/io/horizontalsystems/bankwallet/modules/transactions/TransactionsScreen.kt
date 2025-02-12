package io.horizontalsystems.bankwallet.modules.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.balance.BalanceAccountsViewModel
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule
import io.horizontalsystems.bankwallet.modules.balance.BalanceScreenState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.CellHeaderSorting
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.HSCircularProgressIndicator
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderSorting
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderStick
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.ScrollableTabs
import io.horizontalsystems.bankwallet.ui.compose.components.SectionItemPosition
import io.horizontalsystems.bankwallet.ui.compose.components.SectionUniversalItem
import io.horizontalsystems.bankwallet.ui.compose.components.SelectorDialogCompose
import io.horizontalsystems.bankwallet.ui.compose.components.SelectorItem
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.sectionItemBorder
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.SafeExtend.isSafeIcon

class TransactionsFragment : BaseFragment() {

    private val viewModel by viewModels<TransactionsViewModel> { TransactionsModule.Factory() }
    //    private val viewModel by navGraphViewModels<TransactionsViewModel>(R.id.mainFragment) { TransactionsModule.Factory() }
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
                ComposeAppTheme {
                    TransactionsScreen(findNavController(), viewModel)
                }
            }
        }
    }
}

@Composable
fun TransactionsScreen(
    navController: NavController,
    viewModel: TransactionsViewModel
) {
    val accountsViewModel = viewModel<BalanceAccountsViewModel>(factory = BalanceModule.AccountsFactory())

    val filterTypes by viewModel.filterTypesLiveData.observeAsState()
    val showFilterAlertDot by viewModel.filterResetEnabled.observeAsState(false)

    val uiState = viewModel.uiState
    val syncing = uiState.syncing
    val transactions = uiState.transactions

    val filterBlockchains by viewModel.filterBlockchainsLiveData.observeAsState()
    val filterZeroTransactions by viewModel.filterZeroTransactionLiveData.observeAsState()


    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                title = stringResource(R.string.Transactions_Title),
                showSpinner = syncing,
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back button",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Transactions_Filter),
                        icon = R.drawable.ic_manage_2_24,
                        showAlertDot = showFilterAlertDot,
                        onClick = {
                            navController.slideFromRight(R.id.transactionFilterFragment)
                        },
                    )
                )
            )
            filterTypes?.let { filterTypes ->
                FilterTypeTabs(
                    filterTypes = filterTypes,
                    onTransactionTypeClick = viewModel::setFilterTransactionType
                )
            }


            filterBlockchains?.let { filterBlockchains ->
                CellHeaderSorting(borderBottom = true) {
                    var showFilterBlockchainDialog by remember { mutableStateOf(false) }
                    var showFilterTransaction by remember { mutableStateOf(false) }
                    if (showFilterBlockchainDialog) {
                        SelectorDialogCompose(
                                title = stringResource(R.string.Transactions_Filter_Blockchain),
                                items = filterBlockchains.map {
                                    SelectorItem(it.item?.name ?: stringResource(R.string.Transactions_Filter_AllBlockchains), it.selected, it)
                                },
                                onDismissRequest = {
                                    showFilterBlockchainDialog = false
                                },
                                onSelectItem = viewModel::onEnterFilterBlockchain
                        )
                    }

                    if (showFilterTransaction) {
                        filterZeroTransactions?.let { filterZeroTransactions ->
                            SelectorDialogCompose(
                                    title = stringResource(R.string.Zero_Transactions_Filter),
                                    items = filterZeroTransactions.map {
                                        SelectorItem(it.item, it.selected, it)
                                    },
                                    onDismissRequest = {
                                        showFilterTransaction = false
                                    },
                                    onSelectItem = viewModel::setFilterZeroIncomingTransaction
                            )
                        }
                    }

                    val filterBlockchain = filterBlockchains.firstOrNull { it.selected }?.item
                    val filterZeroTransaction = filterZeroTransactions?.firstOrNull { it.selected }?.item
                    Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ButtonSecondaryTransparent(
                                title = filterBlockchain?.name ?: stringResource(R.string.Transactions_Filter_AllBlockchains),
                                iconRight = R.drawable.ic_down_arrow_20,
                                onClick = {
                                    showFilterBlockchainDialog = true
                                }
                        )

                        ButtonSecondaryTransparent(
                                title = filterZeroTransaction ?: stringResource(R.string.Transaction_Non_Zero_Transaction),
                                iconRight = R.drawable.ic_down_arrow_20,
                                onClick = {
                                    showFilterTransaction = true
                                }
                        )
                    }
                }
            }


            Crossfade(uiState.viewState, label = "") { viewState ->
                if (viewState == ViewState.Success) {
                    transactions?.let { transactionItems ->
                        if (transactionItems.isEmpty()) {
                            if (syncing) {
                                ListEmptyView(
                                    text = stringResource(R.string.Transactions_WaitForSync),
                                    icon = R.drawable.ic_clock
                                )
                            } else {
                                ListEmptyView(
                                    text = stringResource(R.string.Transactions_EmptyList),
                                    icon = R.drawable.ic_outgoingraw
                                )
                            }
                        } else {
                            val listState = rememberSaveable(
                                uiState.transactionListId,
                                (accountsViewModel.balanceScreenState as? BalanceScreenState.HasAccount)?.accountViewItem?.id,
                                saver = LazyListState.Saver
                            ) {
                                LazyListState(0, 0)
                            }

                            val onClick: (TransactionViewItem) -> Unit = remember {
                                {
                                    onTransactionClick(
                                        it,
                                        viewModel,
                                        navController
                                    )
                                }
                            }

                            LazyColumn(state = listState,
                                    modifier = Modifier.wrapContentHeight().padding(vertical = 16.dp, horizontal = 16.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(ComposeAppTheme.colors.lawrence)) {
                                transactionList(
                                    transactionsMap = transactionItems,
                                    willShow = { viewModel.willShow(it) },
                                    onClick = onClick,
                                    onBottomReached = { viewModel.onBottomReached() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun onTransactionClick(
    transactionViewItem: TransactionViewItem,
    viewModel: TransactionsViewModel,
    navController: NavController
) {
    val transactionItem = viewModel.getTransactionItem(transactionViewItem) ?: return

//    viewModel.tmpItemToShow = transactionItem
    App.tmpItemToShow = transactionItem
    App.tmpItemToShow?.let {
        navController.slideFromBottom(R.id.transactionInfoFragment)
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.transactionList(
    transactionsMap: Map<String, List<TransactionViewItem>>,
    willShow: (TransactionViewItem) -> Unit,
    onClick: (TransactionViewItem) -> Unit,
    onBottomReached: () -> Unit
) {
    val bottomReachedUid = getBottomReachedUid(transactionsMap)

    transactionsMap.forEach { (dateHeader, transactions) ->
        stickyHeader {
            HeaderStick(text = dateHeader)
        }

        val itemsCount = transactions.size
        val singleElement = itemsCount == 1

        itemsIndexed(
            items = transactions,
            key = { _, item ->
                item.uid
            }
        ) { index, item ->
            val position: SectionItemPosition = when {
                singleElement -> SectionItemPosition.Single
                index == 0 -> SectionItemPosition.First
                index == itemsCount - 1 -> SectionItemPosition.Last
                else -> SectionItemPosition.Middle
            }

            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                TransactionCell(item, position) { onClick.invoke(item) }
            }

            willShow.invoke(item)

            if (item.uid == bottomReachedUid) {
                onBottomReached.invoke()
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    item {
        Spacer(modifier = Modifier.height(20.dp))
    }
}

private fun getBottomReachedUid(transactionsMap: Map<String, List<TransactionViewItem>>): String? {
    val txList = transactionsMap.values.flatten()
    //get index not exact bottom but near to the bottom, to make scroll smoother
    val index = if (txList.size > 4) txList.size - 4 else 0

    return txList.getOrNull(index)?.uid
}

@Composable
fun DateHeader(dateHeader: String) {
    HeaderSorting(borderTop = false, borderBottom = true, isModifyBg = true) {
        subhead1_grey(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = dateHeader,
            maxLines = 1,
        )
    }
}

@Composable
fun TransactionCell(item: TransactionViewItem, position: SectionItemPosition, onClick: () -> Unit) {
    val divider = position == SectionItemPosition.Middle || position == SectionItemPosition.Last
    SectionUniversalItem(
        borderTop = divider,
    ) {
        val clipModifier = when (position) {
            SectionItemPosition.First -> {
                Modifier.clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            }
            SectionItemPosition.Last -> {
                Modifier.clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            }
            SectionItemPosition.Single -> {
                Modifier.clip(RoundedCornerShape(12.dp))
            }
            else -> Modifier
        }

        val borderModifier = if (position != SectionItemPosition.Single) {
            Modifier.sectionItemBorder(1.dp, ComposeAppTheme.colors.steel20, 12.dp, position)
        } else {
            Modifier.border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(12.dp))
        }

        RowUniversal(
            modifier = Modifier
                .fillMaxSize()
                .then(clipModifier)
                .then(borderModifier)
                .background(ComposeAppTheme.colors.lawrence)
                .clickable(onClick = onClick),
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(42.dp)
                    .alpha(if (item.spam) 0.5f else 1f),
                contentAlignment = Alignment.Center
            ) {
                item.progress?.let { progress ->
                    HSCircularProgressIndicator(progress)
                }
                val icon = item.icon
                when (icon) {
                    TransactionViewItem.Icon.Failed -> {
                        Icon(
                            painter = painterResource(R.drawable.ic_attention_24),
                            tint = ComposeAppTheme.colors.lucian,
                            contentDescription = null
                        )
                    }
                    is TransactionViewItem.Icon.Platform -> {
                        if (icon.iconRes == R.drawable.logo_safe_24) {
                            Image(
                                    modifier = Modifier.size(32.dp),
                                    painter = painterResource(icon.iconRes),
                                    contentDescription = null,
                            )
                        } else {
                            Icon(
                                    modifier = Modifier.size(32.dp),
                                    painter = painterResource(icon.iconRes ?: R.drawable.coin_placeholder),
                                    tint = ComposeAppTheme.colors.leah,
                                    contentDescription = null
                            )
                        }
                    }
                    is TransactionViewItem.Icon.Regular -> {
                        val shape = if (icon.rectangle) RoundedCornerShape(CornerSize(4.dp)) else CircleShape
                        if (icon.url.isSafeIcon()) {
                            Image(painter = painterResource(id = R.drawable.logo_safe_24),
                                contentDescription = null,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(32.dp)
                                    .clip(shape))
                        } else {
                            CoinImage(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(shape),
                                iconUrl = icon.url,
                                placeholder = icon.placeholder
                            )
                        }
                    }
                    is TransactionViewItem.Icon.Double -> {
                        val backShape = if (icon.back.rectangle) RoundedCornerShape(CornerSize(4.dp)) else CircleShape
                        val frontShape = if (icon.front.rectangle) RoundedCornerShape(CornerSize(4.dp)) else CircleShape
                        if (icon.back.url.isSafeIcon()) {
                            Image(painter = painterResource(id = R.drawable.logo_safe_24),
                                contentDescription = null,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(top = 6.dp, start = 6.dp)
                                    .size(24.dp)
                                    .clip(backShape))
                        } else {
                            CoinImage(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(top = 4.dp, start = 6.dp)
                                    .size(24.dp)
                                    .clip(backShape),
                                iconUrl = icon.back.url,
                                placeholder = icon.back.placeholder,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 4.5.dp, end = 6.5.dp)
                                .size(24.dp)
                                .clip(frontShape)
                                .background(ComposeAppTheme.colors.tyler)
                        )
                        if (icon.front.url.isSafeIcon()) {
                            Image(painter = painterResource(id = R.drawable.logo_safe_24),
                                contentDescription = null,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(bottom = 6.dp, end = 6.dp)
                                    .size(20.dp)
                                    .clip(frontShape))
                        } else {
                            CoinImage(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(bottom = 4.dp, end = 6.dp)
                                    .size(24.dp)
                                    .clip(frontShape),
                                iconUrl = icon.front.url,
                                placeholder = icon.front.placeholder,
                            )
                        }
                    }
                    is TransactionViewItem.Icon.ImageResource -> {}
                }
            }
            Column(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .alpha(if (item.spam) 0.5f else 1f)
            ) {
                Row {
                    body_leah(
                        modifier = Modifier.padding(end = 32.dp),
                        text = item.title,
                        maxLines = 1,
                    )
                    Spacer(Modifier.weight(1f))
                    item.primaryValue?.let { coloredValue ->
                        Text(
                            text = if (item.showAmount) coloredValue.value else "*****",
                            style = ComposeAppTheme.typography.body,
                            color = coloredValue.color.compose(),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    }

                    if (item.doubleSpend) {
                        Image(
                            modifier = Modifier.padding(start = 6.dp),
                            painter = painterResource(R.drawable.ic_double_spend_20),
                            contentDescription = null
                        )
                    }
                    item.locked?.let { locked ->
                        Image(
                            modifier = Modifier.padding(start = 6.dp),
                            painter = painterResource(if (locked) R.drawable.ic_lock_20 else R.drawable.ic_unlock_20),
                            contentDescription = null
                        )
                    }
                    if (item.sentToSelf) {
                        Image(
                            modifier = Modifier.padding(start = 6.dp),
                            painter = painterResource(R.drawable.ic_arrow_return_20),
                            contentDescription = null
                        )
                    }
                }
                Spacer(Modifier.height(1.dp))
                Row {
                    subhead2_grey(
                        text = item.subtitle,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        maxLines = 2,
                    )
                    item.secondaryValue?.let { coloredValue ->
                        Text(
                            text = if (item.showAmount) coloredValue.value else "*****",
                            style = ComposeAppTheme.typography.subhead2,
                            color = coloredValue.color.compose(),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTypeTabs(
    filterTypes: List<Filter<FilterTransactionType>>,
    onTransactionTypeClick: (FilterTransactionType) -> Unit
) {
    val tabItems = filterTypes.map {
        TabItem(stringResource(it.item.title), it.selected, it.item)
    }

    ScrollableTabs(tabItems) { transactionType ->
        onTransactionTypeClick.invoke(transactionType)
    }
}

data class Filter<T>(val item: T, val selected: Boolean)
