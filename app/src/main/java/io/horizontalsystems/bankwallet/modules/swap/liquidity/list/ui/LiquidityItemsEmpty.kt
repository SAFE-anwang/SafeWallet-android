package io.horizontalsystems.bankwallet.modules.balance.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.balance.AccountViewItem
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.ScreenMessageWithAction

@Composable
fun LiquidityItemsEmpty(navController: NavController) {

    ScreenMessageWithAction(
        text = stringResource(R.string.Liquidity_NoAlert),
        icon = R.drawable.ic_add_to_wallet_2_48
    ) {

    }

}
