package io.horizontalsystems.bankwallet.modules.send.submodules.address

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.SendSubmoduleFragment
import io.horizontalsystems.bankwallet.modules.swap.settings.RecipientAddressViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.marketkit.models.Token

class SendAddressFragment(
    private val token: Token,
    private val addressModuleDelegate: SendAddressModule.IAddressModuleDelegate,
    private val sendHandler: SendModule.ISendHandler
) : SendSubmoduleFragment() {

    private val viewModel by activityViewModels<RecipientAddressViewModel> {
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
                    HSAddressInput(
                        modifier = Modifier.padding(top = 12.dp),
                        tokenQuery = token.tokenQuery,
                        coinCode = token.coin.code,
                        navController= findNavController(),
                        error = viewModel.error
                    ) {
                        viewModel.setAddress(it)
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
