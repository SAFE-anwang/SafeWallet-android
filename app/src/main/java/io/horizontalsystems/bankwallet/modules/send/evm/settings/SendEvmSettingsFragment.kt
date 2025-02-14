package io.horizontalsystems.bankwallet.modules.send.evm.settings

import android.os.Parcelable
import androidx.annotation.IdRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.send.evm.confirmation.SendEvmConfirmationViewModel
import kotlinx.android.parcel.Parcelize

class SendEvmSettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        SendEvmSettingsScreen(navController)
    }

    @Parcelize
    data class Input(@IdRes val navGraphId: Int) : Parcelable
}

@Composable
fun SendEvmSettingsScreen(navController: NavController) {
    val viewModelStoreOwner = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry(R.id.sendEvmConfirmationFragment)
    }

    val viewModel = viewModel<SendEvmConfirmationViewModel>(
        viewModelStoreOwner = viewModelStoreOwner,
    )

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetSettingsContent(navController)
}
