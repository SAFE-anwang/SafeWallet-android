package io.horizontalsystems.bankwallet.modules.safe4.node.proposal.create

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourCreateNodeModule
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.SafeFourProposalModule

class SafeFourCreateProposalFragment: BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val wallet = navController.getInput<Wallet>()
        if (wallet == null) {
            Toast.makeText(App.instance, "Wallet is Null", Toast.LENGTH_SHORT).show()
            navController.popBackStack(R.id.nodeListFragment, true)
            return
        }
        val viewModel by viewModels<SafeFourCreateProposalViewModel> { SafeFourProposalModule.Factory(wallet) }
        SafeFourCreateProposalScreen(viewModel = viewModel, navController = navController, isSuper = true)
    }
}