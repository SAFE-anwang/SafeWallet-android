package io.horizontalsystems.bankwallet.modules.balance.token

import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItem
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeType
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourModule
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.SafeFourProposalModule
import io.horizontalsystems.bankwallet.modules.safe4.node.reward.SafeFourRewardModule
import io.horizontalsystems.bankwallet.modules.safe4.node.safe3.RedeemSafe3Module
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.marketkit.models.BlockchainType

@Composable
fun SafeFourButtonsRow(
    navController: NavController,
    viewModel: TokenBalanceViewModel,
    viewItem: BalanceViewItem
) {
    Row(
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 16.dp)
    ) {
        ButtonPrimaryYellow(
                modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                title = stringResource(R.string.Safe_Four_Super_Node),
                onClick = {
                    navController.slideFromBottom(
                            R.id.nodeListFragment,
                            SafeFourModule.Input(R.string.Safe_Four_Super_Node, NodeType.SuperNode.ordinal, viewModel.wallet)
                    )
                },
                enabled = viewItem.sendEnabled
        )
        HSpacer(8.dp)

        ButtonPrimaryYellow(
                modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                title = stringResource(R.string.Safe_Four_Master_Node),
                onClick = {
                    navController.slideFromBottom(
                            R.id.nodeListFragment,
                            SafeFourModule.Input(R.string.Safe_Four_Master_Node, NodeType.MainNode.ordinal, viewModel.wallet)
                    )
                },
                enabled = viewItem.sendEnabled
        )

        HSpacer(8.dp)

        ButtonPrimaryYellow(
                modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                title = stringResource(R.string.Safe_Four_Proposal),
                onClick = {
                    navController.slideFromBottom(
                            R.id.proposalFragment,
                            SafeFourProposalModule.Input(viewModel.wallet)
                    )
                },
                enabled = viewItem.sendEnabled
        )
    }
    Row(
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 16.dp)
    ) {

        ButtonPrimaryYellow(
                modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                title = stringResource(R.string.Safe_Four_Profit),
                onClick = {
                    navController.slideFromBottom(
                            R.id.rewardFragment,
                            SafeFourRewardModule.Input(viewModel.wallet)
                    )
                },
                enabled = viewItem.sendEnabled
        )
        HSpacer(8.dp)
        ButtonPrimaryYellow(
                modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                title = stringResource(R.string.Redeem_Safe3_Title),
                onClick = {
                    val walletList: List<Wallet> = App.walletManager.activeWallets
                    var safeWallet: Wallet? = null
                    for (it in walletList) {
                        if (it.token.blockchain.type is BlockchainType.Safe && it.coin.uid == "safe-coin") {
                            safeWallet = it
                        }
                    }
                    if (safeWallet == null) {
                        Toast.makeText(navController.context, Translator.getString(R.string.Safe4_Wallet_Tips, "Safe"), Toast.LENGTH_SHORT).show()
                        return@ButtonPrimaryYellow
                    }
                    navController.slideFromBottom(
                            R.id.redeemSafe3Fragment,
                            RedeemSafe3Module.Input(viewModel.wallet, safeWallet)
                    )
                },
                enabled = viewItem.sendEnabled
        )
    }
}