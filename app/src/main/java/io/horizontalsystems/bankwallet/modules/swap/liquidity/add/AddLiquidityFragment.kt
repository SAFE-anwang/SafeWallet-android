package io.horizontalsystems.bankwallet.modules.swap.liquidity.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromBottomForResult
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.eip20approve.Eip20ApproveConfirmFragment
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionService
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.approve.confirmation.SwapApproveConfirmationFragment
import io.horizontalsystems.bankwallet.modules.swap.approve.confirmation.SwapApproveConfirmationModule
import io.horizontalsystems.bankwallet.modules.swap.liquidity.add.ui.AddLiquidityScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.findNavController

class AddLiquidityFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            try {
                val factory = AddLiquidityModule.Factory(requireArguments())
                val viewModel: AddLiquidityViewModel by viewModels { factory }

                viewModel.sendStateObservable.observe(viewLifecycleOwner, Observer {
                    if (it is SendEvmTransactionService.SendState.Sent) {
                        Toast.makeText(
                            requireActivity(),
                            R.string.Liquidity_Add_Success,
                            Toast.LENGTH_LONG
                        ).show()
                        findNavController().navigateUp()
                    }
                })

                setContent {
                    ComposeAppTheme {
                        AddLiquidityNavHost(
                            navController = findNavController(),
                            viewModel = viewModel,
                        )
                    }
                }
            } catch (t: Throwable) {
                Toast.makeText(
                    App.instance, t.message ?: t.javaClass.simpleName, Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

@Composable
private fun AddLiquidityNavHost(
    navController: NavController,
    viewModel: AddLiquidityViewModel,
) {
    AddLiquidityScreen(
        navController = navController,
        viewModel = viewModel,
        onTapRevoke1 = {
            viewModel.revokeEvmData?.let { revokeEvmData ->
                navController.slideFromBottomForResult<SwapApproveConfirmationFragment.Result>(
                    R.id.swapApproveConfirmationFragment,
                    SwapApproveConfirmationModule.Input(
                        revokeEvmData,
                        viewModel.state.dex.blockchainType,
                        false
                    )
                ) {
                    if (it.approved) {
                        viewModel.didApprove()
                    }
                }
            }
        },
        onTapApprove1 = {
            viewModel.approveData?.let { data ->
                navController.slideFromBottomForResult<Eip20ApproveConfirmFragment.Result>(
                    R.id.eip20ApproveFragment,
                    data
                ) {
                    viewModel.didApprove()
                }
            }
        },
        onTapRevoke2 = {
            viewModel.revokeEvmDataB?.let { revokeEvmData ->
                navController.slideFromBottomForResult<SwapApproveConfirmationFragment.Result>(
                    R.id.swapApproveConfirmationFragment,
                    SwapApproveConfirmationModule.Input(
                        revokeEvmData,
                        viewModel.state.dex.blockchainType,
                        false
                    )
                ) {
                    if (it.approved) {
                        viewModel.didApproveB()
                    }
                }
            }
        },
        onTapApprove2 = {
            viewModel.approveDataB?.let { data ->
                navController.slideFromBottomForResult<Eip20ApproveConfirmFragment.Result>(
                    R.id.eip20ApproveFragment,
                    data
                ) {
                    viewModel.didApproveB()
                }
            }
        },
        onTapProceed = {
            viewModel.getSendEvmData()?.let { sendEvmData ->
                navController.slideFromRight(
                    R.id.liquidityConfirmationFragment,
                    io.horizontalsystems.bankwallet.modules.swap.liquidity.confirmation.LiquidityConfirmationFragment.Input(
                        viewModel.state.dex,
                        SendEvmModule.TransactionDataParcelable(sendEvmData.transactionData),
                        sendEvmData.additionalInfo,
                        viewModel.getFromToken
                    )
                )
            }
        }
    )
}
