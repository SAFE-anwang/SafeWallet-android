package io.horizontalsystems.bankwallet.modules.safe4.safe2wsafe.address

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.SendSubmoduleFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.address.SendAddressModule
import io.horizontalsystems.bankwallet.modules.swap.settings.RecipientAddressViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.Token

class WsafeAddressFragment(
    private val wsafeAddress: String,
    private val token: Token,
    private val addressModuleDelegate: SendAddressModule.IAddressModuleDelegate,
    private val sendHandler: SendModule.ISendHandler
) : SendSubmoduleFragment() {

    private val viewModel by viewModels<RecipientAddressViewModel> {
        SendAddressModule.Factory(sendHandler, addressModuleDelegate)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            // Dispose the Composition when the view's LifecycleOwner
            // is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ComposeAppTheme {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.Safe4_Wsafe_Receive_Address),
                            style = ComposeAppTheme.typography.subhead1,
                            color = ComposeAppTheme.colors.leah,
                            maxLines = 1
                        )
                        HSAddressInput(
                            modifier = Modifier.padding(top = 12.dp),
                            initial = Address(wsafeAddress),
                            tokenQuery = token.tokenQuery,
                            navController = findNavController(),
                            coinCode = token.coin.code,
                            error = viewModel.error,
                            onValueChange = {
                                viewModel.setAddress(it)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun init() {
        // need to init to set lateinit properties
        viewModel
    }
}
