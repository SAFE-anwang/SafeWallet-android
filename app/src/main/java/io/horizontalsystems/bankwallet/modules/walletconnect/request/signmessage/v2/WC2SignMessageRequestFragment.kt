package io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.v2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.core.findNavController

class WC2SignMessageRequestFragment  {

    /*val vmFactory by lazy {
        WCSignMessageRequestModule.FactoryWC2(
            App.wc2SessionManager.createRequestData(requireArguments().getLong(REQUEST_ID_KEY))
        )
    }
    private val viewModel by viewModels<WCSignMessageRequestViewModel> { vmFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                SignMessageRequestScreen(
                    findNavController(),
                    viewModel
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            viewModel.reject()
        }

        viewModel.closeLiveEvent.observe(viewLifecycleOwner) {
            findNavController().popBackStack()
        }
    }

    companion object {
        private const val REQUEST_ID_KEY = "request_id_key"

        fun prepareParams(requestId: Long) =
            bundleOf(REQUEST_ID_KEY to requestId)
    }*/

}
