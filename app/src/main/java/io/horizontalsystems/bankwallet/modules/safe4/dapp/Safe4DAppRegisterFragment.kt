package io.horizontalsystems.bankwallet.modules.safe4.dapp

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.slideFromRight

class Safe4DAppRegisterFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val existingDApp = navController.getInput<ManagedDAppItem>()
        val input = Safe4DAppModule.RegisterInput(
            existingDApp = existingDApp,
            walletAddress = ""
        )

        Safe4DAppRegisterScreen(
            navController = navController,
            viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = Safe4DAppModule.FactoryRegister(input)
            )
        )
    }

    companion object {
        fun handler(navController: NavController, existingDApp: ManagedDAppItem? = null) {
            navController.slideFromRight(R.id.safe4DAppRegisterFragment, existingDApp)
        }
    }
}
