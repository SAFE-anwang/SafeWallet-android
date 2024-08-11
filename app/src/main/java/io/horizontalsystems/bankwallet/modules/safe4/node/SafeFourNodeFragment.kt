package io.horizontalsystems.bankwallet.modules.safe4.node

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.marketkit.models.BlockchainType

class SafeFourNodeFragment: BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {

        val address = App.evmBlockchainManager.getEvmKitManager(BlockchainType.SafeFour).evmKitWrapper?.evmKit?.receiveAddress
        if (address == null) {
            Toast.makeText(App.instance, "Wallet is Null", Toast.LENGTH_SHORT).show()
            navController.popBackStack(R.id.nodeListFragment, true)
            return
        }
        val input = navController.getInput<SafeFourModule.Input>()
        val title = input?.titleRes ?: R.string.Safe_Four_Super_Node
        val nodeType = input?.nodeType ?: NodeType.SuperNode.ordinal
        val wallet = input?.wallet ?: return
        val viewModel by viewModels<SafeFourNodeViewModel> { SafeFourModule.Factory(address, nodeType, getString(title), wallet) }

        TabScreen(
                viewModel,
                navController
        )
    }
}