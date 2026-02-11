package io.horizontalsystems.bankwallet.modules.safe4.safeprice

import android.annotation.SuppressLint
import android.os.Parcelable
import android.util.Log
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.layout.Spacer
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.CustomToken
import io.horizontalsystems.bankwallet.modules.safe4.node.HintView
import io.horizontalsystems.bankwallet.modules.safe4.src20.DeployType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.SectionItemPosition
import io.horizontalsystems.bankwallet.ui.compose.components.SectionUniversalItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_green50
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.body_issykBlue
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.sectionItemBorder
import io.horizontalsystems.bankwallet.ui.compose.components.title2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.title3_leah
import io.horizontalsystems.bankwallet.ui.compose.components.title3_remus
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.Token
import kotlinx.parcelize.Parcelize

class PriceFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<Wallet>()
        val viewModel = SRC20InfoViewModel(input)
        ContentScreen(navController, viewModel)
    }
}

@Composable
fun ContentScreen(navController: NavController, viewModel: SRC20InfoViewModel) {
    val uiState = viewModel.uiState
    val view = LocalView.current
    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(R.string.SRC20_Info_Title),
                navigationIcon = {
                    HsBackButton(onClick = navController::popBackStack)
                },
            )
        },
        backgroundColor = ComposeAppTheme.colors.tyler,
    ) { paddingValues ->
        val listState = rememberLazyListState()
        LazyColumn(Modifier.padding(paddingValues), state = listState) {
            item {
                body_leah(stringResource(R.string.SRC20_USDT_Price),
                    modifier = Modifier.padding(start = 16.dp))
            }
            uiState.price?.let {
                priceList(it)
            }
            uiState.customToken?.let {
                item {
                    VSpacer(16.dp)
                    body_leah(stringResource(R.string.SRC20_Info_Title),
                        modifier = Modifier.padding(start = 16.dp))
                    TokenInfo(view, it, uiState.totalSupply ?: "", uiState.description ?: "")
                    VSpacer(16.dp)
                }
            }
        }
    }

}

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.priceList(prices: List<MarketPrice>) {
    itemsIndexed(
        items = prices
    ) { index, item ->
        val itemsCount = prices.size
        val singleElement = itemsCount == 1
        val position: SectionItemPosition = when {
            singleElement -> SectionItemPosition.Single
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
                    Modifier.sectionItemBorder(
                        1.dp,
                        ComposeAppTheme.colors.steel20,
                        12.dp,
                        position
                    )
                } else {
                    Modifier.border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(12.dp))
                }

                RowUniversal(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(clipModifier)
                        .then(borderModifier)
                        .background(ComposeAppTheme.colors.lawrence),
                ) {
                    Row(
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                        )
                    ) {
                        CoinImage(
                            iconUrl = item.logoURI,
                            placeholder = R.drawable.ic_safe_20,
                            modifier = Modifier
                                .size(32.dp)
                        )
                        HSpacer(8.dp)
                        Column(
                            modifier = Modifier.weight(3f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                body_leah(
                                    text = item.symbol,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Box(
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ComposeAppTheme.colors.jeremy)
                                ) {
                                    androidx.compose.material.Text(
                                        modifier = Modifier.padding(
                                            start = 4.dp,
                                            end = 4.dp,
                                            bottom = 1.dp
                                        ),
                                        text = "SRC20",
                                        color = ComposeAppTheme.colors.bran,
                                        style = ComposeAppTheme.typography.microSB,
                                        maxLines = 1,
                                    )
                                }

                            }
                            Row {
                                item.price?.let {
                                    body_green50(
                                        text = "$${String.format("%.4f", item.price.toFloat())}",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    body_green50(
                                        text = "(${if (item.change.toFloat() >= 0) '+' else '-'}${item.change}%)",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                            }
                        }

                        Text(
                            text = String.format("%.2f", item.usdtReserves.toFloat())
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TokenInfo(
    view: View,
    customToken: CustomToken,
    totalSupply: String,
    description: String
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, end = 16.dp)
        .clip(RoundedCornerShape(8.dp))
        .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))
        .background(ComposeAppTheme.colors.lawrence)
        .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoinImage(
                iconUrl = customToken.logoURI,
                placeholder = R.drawable.ic_safe_20,
                modifier = Modifier
                    .size(32.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                body_grey(stringResource(R.string.SRC20_Deploy_Name))
                VSpacer(4.dp)
                body_leah(customToken.name)
                VSpacer(4.dp)
                body_grey(stringResource(R.string.SRC20_Deploy_Symbol))
                VSpacer(4.dp)
                body_leah(customToken.symbol)
            }
        }
        VSpacer(10.dp)
        Box(
            modifier = Modifier.height(1.dp).fillMaxWidth().background(ComposeAppTheme.colors.grey50)
        )
        VSpacer(10.dp)
        Column {
            body_grey(stringResource(R.string.SRC20_Info_Contact))
            VSpacer(4.dp)
            body_issykBlue(customToken.address,
                modifier = Modifier.clickable {
                    TextHelper.copyText(customToken.address)
                    HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                }
            )
            VSpacer(10.dp)
            Row {
                Column (
                    modifier = Modifier.weight(5f)
                ) {
                    body_grey(stringResource(R.string.SRC20_Deploy_Supply))
                    VSpacer(4.dp)
                    body_leah("${totalSupply}  ${customToken.symbol}")
                }
                Column(
                    modifier = Modifier.weight(2f),
                    horizontalAlignment = Alignment.End
                ) {
                    body_grey(stringResource(R.string.SRC20_Info_Type))
                    VSpacer(4.dp)
                    body_green50(stringResource(if (customToken.type == 0) R.string.SRC20_Info_Type_Not_Add else R.string.SRC20_Info_Type_Add))
                }
            }
        }
        VSpacer(10.dp)
        Box(
            modifier = Modifier.height(1.dp).fillMaxWidth().background(ComposeAppTheme.colors.grey50)
        )
        VSpacer(10.dp)
        HintView(stringResource(R.string.SRC20_Info_Hint))
        VSpacer(10.dp)
        Box(
            modifier = Modifier.height(1.dp).fillMaxWidth().background(ComposeAppTheme.colors.grey50)
        )
        VSpacer(10.dp)
        body_grey(stringResource(R.string.SRC20_Edit_Desc))
        VSpacer(4.dp)
        body_leah(description)
        VSpacer(10.dp)

    }
}

