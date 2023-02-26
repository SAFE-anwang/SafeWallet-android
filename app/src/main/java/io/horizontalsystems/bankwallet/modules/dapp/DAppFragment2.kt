package io.horizontalsystems.bankwallet.modules.dapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.ConcatAdapter
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.overview.Loading
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.core.findNavController
import java.util.HashMap

@Composable
fun DAppFragment2(
    navController: NavController,
    viewModel: DAppViewModel  = viewModel(factory =  DAppModule.Factory())
) {

    val isRefreshing by viewModel.syncingLiveData.observeAsState(false)
    val viewState by viewModel.viewState.observeAsState()
    val viewItems by viewModel.dAppList.observeAsState()

    val scrollState = rememberScrollState()
    Column(){
        CellSingleLineLawrenceSection {
            Column(
                modifier = Modifier.fillMaxSize()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        val bundle = Bundle()
                        bundle.putBoolean("isInput", true)
                        navController.slideFromRight(R.id.dappBrowseFragment, bundle)
                    },
                verticalArrangement = Arrangement.Center
            ) {

                body_leah(
                    text = stringResource(R.string.Access_Websites_Input_Hint),
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
//                Spacer(Modifier.weight(1f))
            }
        }
    HSSwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = {
            viewModel.refresh()
        }
    ) {

        DAppScreen(viewModel, navController)

        Spacer(modifier = Modifier.height(10.dp))



        Spacer(modifier = Modifier.height(10.dp))

        Crossfade(viewState) { viewState ->
            when (viewState) {
                ViewState.Loading -> {
                    Loading()
                }
                is ViewState.Error -> {
                    if (viewItems?.isEmpty() == true)
                    ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
                }
                ViewState.Success -> {
                    viewItems?.let { viewItem ->
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 40.dp)) {
                            viewItem.keys.forEachIndexed { index, s ->
                                item {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 15.dp, end = 12.dp),
                                        text = s,
                                        style = ComposeAppTheme.typography.headline2,
                                        color = ComposeAppTheme.colors.grey
                                    )
                                }

                                items(viewItem[s]!!) { postItem ->
                                    CellItems(
                                        dappItem = postItem
                                    ) {
                                        val bundle = Bundle()
                                        bundle.putString("url", postItem.dlink)
                                        bundle.putString("name", postItem.name)
                                        navController.slideFromRight(R.id.dappBrowseFragment, bundle)
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
                null -> {}
            }
        }
    }
    }

}

@Composable
private fun DAppScreen(
    viewModel: DAppViewModel,
    navController: NavController
) {
    val filterTypes by viewModel.filterTypesLiveData.observeAsState()

//    Surface(color = ComposeAppTheme.colors.tyler) {
        Spacer(modifier = Modifier.height(70.dp))
        Column {
            filterTypes?.let { filterTypes ->
                FilterTypeTabs(
                    filterTypes,
                    { viewModel.setFilterDAppType(it) },
                    {  }
                )
            }
        }
//    }
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


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CellItems(
    dappItem: DAppItem,
    onClick: () -> Unit
) {
    val clickableModifier = when (onClick) {
        null -> Modifier
        else -> Modifier.clickable {
            onClick.invoke()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .then(clickableModifier)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoinImage(
                iconUrl = dappItem.icon,
                placeholder = dappItem.iconPlaceholder,
                modifier = Modifier
                    .size(24.dp)
            )

            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    modifier = Modifier
                        .padding(start = 12.dp, end = 12.dp, top = 5.dp),
                    text = dappItem.name,
                    style = ComposeAppTheme.typography.body,
                    color = ComposeAppTheme.colors.grey
                )
                Spacer(modifier = Modifier.height(3.dp))

                val desc = if (App.languageManager.currentLanguageName.contains("中文"))
                    dappItem.desc else dappItem.descEN
                Text(
                    modifier = Modifier
                        .padding(start = 12.dp, bottom = 12.dp),
                    text = desc,
                    style = ComposeAppTheme.typography.caption,
                    color = ComposeAppTheme.colors.grey
                )
            }

        }
            Divider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

}
