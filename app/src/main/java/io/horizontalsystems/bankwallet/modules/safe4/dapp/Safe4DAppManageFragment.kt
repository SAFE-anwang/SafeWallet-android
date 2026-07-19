package io.horizontalsystems.bankwallet.modules.safe4.dapp

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment

class Safe4DAppManageFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        Safe4DAppManageScreen(navController)
    }

    companion object {
        fun handler(navController: NavController) {
            navController.navigate(R.id.safe4DAppManageFragment)
        }
    }
}
