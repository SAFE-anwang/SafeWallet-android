package io.horizontalsystems.bankwallet.modules.swap.liquidity.list.ui

import android.content.Intent
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.balance.BackupRequiredError
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewModel
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.manageaccount.dialogs.BackupRequiredDialog
import io.horizontalsystems.bankwallet.modules.safe4.safesend.SafeSendActivity
import io.horizontalsystems.bankwallet.modules.send.SendFragment
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.liquidity.LiquidityMainModule
import io.horizontalsystems.bankwallet.modules.swap.liquidity.list.LiquidityListViewModel
import io.horizontalsystems.bankwallet.modules.swap.liquidity.list.LiquidityViewItem
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.DraggableCardSimple
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefaultBlue
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellMultilineClear
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.RateColor
import io.horizontalsystems.bankwallet.ui.compose.components.RateText
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.marketkit.models.BlockchainType

@Composable
fun LiquidityCardSwipable(
    index: Int,
    viewItem: LiquidityViewItem,
    viewModel: LiquidityListViewModel,
    navController: NavController,
    revealed: Boolean,
    onReveal: (Int) -> Unit,
    onConceal: () -> Unit,
    removeCallback: (Int, LiquidityViewItem) -> Unit
) {

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {

        DraggableCardSimple(
            key = index,
            isRevealed = revealed,
            cardOffset = 72f,
            onReveal = {  },
            onConceal = onConceal,
            content = {
                LiquidityCard(index, viewItem, viewModel, navController, removeCallback)
            }
        )
    }
}

@Composable
fun LiquidityCard(
    index: Int,
    viewItem: LiquidityViewItem,
    viewModel: LiquidityListViewModel,
    navController: NavController,
    removeCallback: (Int, LiquidityViewItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {

            }
    ) {
        CellMultilineClear(height = 44.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WalletIcon(viewItem.walletA)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(weight = 1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            body_leah(
                                text = viewItem.walletA.coin.code,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!viewItem.walletA.badge.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ComposeAppTheme.colors.jeremy)
                                ) {
                                    Text(
                                        modifier = Modifier.padding(
                                            start = 4.dp,
                                            end = 4.dp,
                                            bottom = 1.dp
                                        ),
                                        text = viewItem.walletA.badge!!,
                                        color = ComposeAppTheme.colors.bran,
                                        style = ComposeAppTheme.typography.microSB,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(24.dp))

                    }
                    Text(
                        text = viewItem.amountA,
                        color = ComposeAppTheme.colors.leah,
                        style = ComposeAppTheme.typography.headline2,
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
        }
        Divider(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 5.dp, bottom = 6.dp),
            thickness = 1.dp,
            color = if (App.localStorage.currentTheme == ThemeType.Blue) ComposeAppTheme.colors.dividerLine else ComposeAppTheme.colors.steel10
        )
        CellMultilineClear(height = 44.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WalletIcon(viewItem.walletB)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(weight = 1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            body_leah(
                                text = viewItem.walletB.coin.code,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!viewItem.walletB.badge.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ComposeAppTheme.colors.jeremy)
                                ) {
                                    Text(
                                        modifier = Modifier.padding(
                                            start = 4.dp,
                                            end = 4.dp,
                                            bottom = 1.dp
                                        ),
                                        text = viewItem.walletB.badge!!,
                                        color = ComposeAppTheme.colors.bran,
                                        style = ComposeAppTheme.typography.microSB,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(24.dp))

                    }
                    Text(
                        text = viewItem.amountB,
                        color = ComposeAppTheme.colors.leah,
                        style = ComposeAppTheme.typography.headline2,
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
        }

        Divider(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 5.dp, bottom = 6.dp),
            thickness = 1.dp,
            color = if (App.localStorage.currentTheme == ThemeType.Blue) ComposeAppTheme.colors.dividerLine else ComposeAppTheme.colors.steel10
        )
        CellMultilineClear(height = 44.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.weight(weight = 1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.Liquidity_Num),
                            color = ComposeAppTheme.colors.leah,
                            style = ComposeAppTheme.typography.headline2,
                            maxLines = 1,
                        )
                        Text(
                            text = "${viewItem.liquidity}/${viewItem.shareRate}%",
                            color = ComposeAppTheme.colors.leah,
                            style = ComposeAppTheme.typography.headline2,
                            maxLines = 1,
                        )
                    }

                    /*Row(
                        modifier = Modifier.weight(weight = 1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.Liquidity_Total),
                            color = ComposeAppTheme.colors.leah,
                            style = ComposeAppTheme.typography.headline2,
                            maxLines = 1,
                        )
                        Text(
                            text = viewItem.poolTokenTotalSupply.toString(),
                            color = ComposeAppTheme.colors.leah,
                            style = ComposeAppTheme.typography.headline2,
                            maxLines = 1,
                        )
                    }*/
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
        }

        ExpandableContent(index, viewItem, navController, viewModel, removeCallback)
    }
}

@Composable
private fun ExpandableContent(
    index: Int,
    viewItem: LiquidityViewItem,
    navController: NavController,
    viewModel: LiquidityListViewModel,
    removeCallback: (Int, LiquidityViewItem) -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column {
            Divider(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 5.dp, bottom = 6.dp),
                thickness = 1.dp,
                color = if (App.localStorage.currentTheme == ThemeType.Blue) ComposeAppTheme.colors.dividerLine else ComposeAppTheme.colors.steel10
            )
            ButtonsRow(index, viewItem, navController, viewModel, removeCallback)
        }
    }
}

@Composable
private fun ButtonsRow(index: Int,
                       viewItem: LiquidityViewItem,
                       navController: NavController,
                       viewModel: LiquidityListViewModel,
                       removeCallback: (Int, LiquidityViewItem) -> Unit
) {
    Row(
        modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ButtonPrimaryYellow(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.liquidity_remove_title),
            onClick = {
                removeCallback.invoke(index, viewItem)
            },
            enabled = true
        )
        Spacer(modifier = Modifier.width(8.dp))
    }
}

@Composable
private fun WalletIcon(viewItem: Wallet) {
    Box(
        modifier = Modifier
            .width(64.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        if (viewItem.coin.code == "SAFE") {
            Image(painter = painterResource(id = R.drawable.logo_safe_24),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp))
        } else {
            CoinImage(
                iconUrl = viewItem.coin.imageUrl,
                placeholder = viewItem.token.iconPlaceholder,
                modifier = Modifier
                    .size(32.dp)
            )
        }
    }
}

