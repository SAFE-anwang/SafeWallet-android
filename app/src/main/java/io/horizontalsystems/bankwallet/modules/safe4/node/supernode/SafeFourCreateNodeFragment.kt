package io.horizontalsystems.bankwallet.modules.safe4.node.supernode

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
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourModule

class SafeFourCreateNodeFragment: BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<SafeFourModule.CreateInput>()
        val wallet = input?.wallet
        if (wallet == null) {
            navController.popBackStack(R.id.nodeListFragment, true)
            return
        }
        val viewModel by viewModels<SafeFourCreateNodeViewModel> { SafeFourCreateNodeModule.Factory(wallet, input.isSuper) }
        SafeFourCreateNodeScreen(viewModel = viewModel, navController = navController, isSuper = input.isSuper)
    }
}