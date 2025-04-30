package io.horizontalsystems.bankwallet.modules.safe4.node.safe3

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.modules.address.AddressParserModule
import io.horizontalsystems.bankwallet.modules.address.AddressParserViewModel
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_bran
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_grey
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper

class RedeemSafe3LocalFragment(): BaseComposeFragment() {
	@Composable
	override fun GetContent(navController: NavController) {
		val input = navController.getInput<RedeemSafe3Module.Input>()
		val wallet = input?.wallet ?: return

		val viewModel by viewModels<RedeemSafe3LocalViewModel> { RedeemSafe3Module.Factory(wallet) }

		RedeemSafe3LocalScreen(viewModel = viewModel) {

		}
	}
}

@Composable
fun RedeemSafe3LocalScreen(
		viewModel: RedeemSafe3LocalViewModel,
		onStatus: (Boolean) -> Unit
) {
	val uiState = viewModel.uiState
	val safe3Wallet = viewModel.safe3Wallet
	val currentStep = uiState.step
	val syncing = uiState.syncing
	val list = uiState.list
	val sendResult = viewModel.sendResult
	val view = LocalView.current
	when (sendResult) {
		SendResult.Sending -> {
			HudHelper.showInProcessMessage(
					view,
					R.string.Redeem_Safe3_Send_Sending,
					SnackbarDuration.INDEFINITE
			)
		}

		SendResult.Sent -> {
			HudHelper.showSuccessMessage(
					view,
					R.string.Redeem_Safe3_Send_Success,
					SnackbarDuration.LONG
			)
			viewModel.sendResult = null
		}

		is SendResult.Failed -> {
			HudHelper.showErrorMessage(view, sendResult.caution.getString())
			viewModel.sendResult = null
		}

		null -> Unit
	}

	Column(modifier = Modifier
			.background(color = ComposeAppTheme.colors.tyler)) {
		onStatus.invoke(syncing)
		/*AppBar(
				title = stringResource(id = R.string.Redeem_Safe3_Local_Wallet),
				showSpinner = syncing,
				navigationIcon = {
					HsBackButton(onClick = { navController.popBackStack() })
				}
		)*/
		if (uiState.isRedeemSuccess) {
			ListEmptyView(
					text = stringResource(R.string.Redeem_Safe4_Redeem_Success),
					icon = R.drawable.ic_no_data
			)
		} else {
			Column {

		Column(modifier = Modifier
				.padding(16.dp)
				.weight(5f)
				.verticalScroll(rememberScrollState())) {
			Column(
					modifier = Modifier
							.clip(RoundedCornerShape(8.dp))
							.border(1.dp, ComposeAppTheme.colors.lawrence, RoundedCornerShape(8.dp))
							.background(ComposeAppTheme.colors.lawrence)
							.fillMaxWidth()
			) {
				Row(
						verticalAlignment = Alignment.CenterVertically
				) {
					Icon(
							painter = painterResource(id = R.drawable.ic_info_20), contentDescription = null,
							modifier = Modifier
									.padding(start = 16.dp)
									.width(24.dp)
									.height(24.dp))
					Text(
							modifier = Modifier
									.padding(16.dp),
							text = stringResource(id = R.string.Redeem_Safe3_Redeem_Desc),
							fontSize = 14.sp
					)
				}
			}

			Spacer(modifier = Modifier.height(16.dp))
			Row(
					verticalAlignment = Alignment.CenterVertically
			) {
				StepView("1", stringResource(id = R.string.Redeem_Safe3_Query), currentStep == 1, currentStep > 1)

				Divider(
						modifier = Modifier
								.weight(1f),
						thickness = 1.dp,
						color = ComposeAppTheme.colors.steel10,
				)
				StepView("2", stringResource(id = R.string.Redeem_Safe3_Verify_Private_Key), currentStep == 2, currentStep > 2)
				Divider(
						modifier = Modifier
								.weight(1f),
						thickness = 1.dp,
						color = ComposeAppTheme.colors.steel10,
				)
				StepView("3", stringResource(id = R.string.Redeem_Safe3_Migration), currentStep == 3, currentStep > 3)
			}

			Spacer(modifier = Modifier.height(8.dp))
			uiState.balance?.let {
				Row(
					verticalAlignment = Alignment.CenterVertically
				) {
					Text(
						modifier = Modifier,
						text = stringResource(id = R.string.Redeem_AvailableBalance, it),
						fontSize = 16.sp
					)
					Spacer(modifier = Modifier.width(16.dp))
					Text(
						modifier = Modifier,
						text = stringResource(id = R.string.Redeem_Enable, uiState.redeemableBalance),
						fontSize = 16.sp
					)
				}
			}
			Spacer(modifier = Modifier.height(8.dp))
			uiState.locked?.let {
				Row(
					verticalAlignment = Alignment.CenterVertically
				) {
					Text(
						modifier = Modifier,
						text = stringResource(id = R.string.Redeem_Lock_Balance, it),
						fontSize = 16.sp
					)
					Spacer(modifier = Modifier.width(16.dp))
					Text(
						modifier = Modifier,
						text = stringResource(id = R.string.Redeem_Enable, uiState.redeemableLocked),
						fontSize = 16.sp
					)
				}
			}

			Spacer(modifier = Modifier.height(16.dp))

			list.forEach {
				Column(
						modifier = Modifier
								.clip(RoundedCornerShape(8.dp))
								.border(1.dp, ComposeAppTheme.colors.lawrence, RoundedCornerShape(8.dp))
								.background(ComposeAppTheme.colors.lawrence)
								.fillMaxWidth()
								.padding(16.dp)
				) {
					body_bran(text = stringResource(id = R.string.Redeem_Safe3_Address_Hint_2, it.address))
					Spacer(modifier = Modifier.height(2.dp))
					body_grey(text = stringResource(id = R.string.Redeem_Safe3_Account_Balance))

					Spacer(modifier = Modifier.height(6.dp))
					body_bran(text = it.safe3Balance,
							textDecoration = if (it.existAvailable) TextDecoration.None else TextDecoration.LineThrough)

					Spacer(modifier = Modifier.height(16.dp))
					Row {
						body_grey(text = stringResource(id = R.string.Redeem_Safe3_Lock_Balance))
						Spacer(modifier = Modifier.width(16.dp))
						body_grey(text = stringResource(id = R.string.Redeem_Safe3_Lock_Num, it.safe3LockNum))
					}

					Spacer(modifier = Modifier.height(6.dp))
					body_bran(text = it.safe3LockBalance,
							textDecoration = if (it.existLocked) TextDecoration.None else TextDecoration.LineThrough)
					if (it.masterNodeLock != null) {
						Spacer(modifier = Modifier.height(16.dp))
						body_grey(text = stringResource(id = R.string.Redeem_Safe3_Master_Balance))

						Spacer(modifier = Modifier.height(6.dp))
						body_bran(text = it.masterNodeLock,
								textDecoration = if (it.existLocked) TextDecoration.None else TextDecoration.LineThrough)
					}
				}
				Spacer(modifier = Modifier.height(3.dp))
			}

			if (!syncing) {

				Spacer(modifier = Modifier.height(16.dp))
				Column(
						modifier = Modifier
								.clip(RoundedCornerShape(8.dp))
								.border(1.dp, ComposeAppTheme.colors.lawrence, RoundedCornerShape(8.dp))
								.background(ComposeAppTheme.colors.lawrence)
								.fillMaxWidth()
				) {
					Row(
							verticalAlignment = Alignment.CenterVertically
					) {
						Icon(
								painter = painterResource(id = R.drawable.ic_info_20), contentDescription = null,
								modifier = Modifier
										.padding(start = 16.dp)
										.width(24.dp)
										.height(24.dp))
						Text(
								modifier = Modifier
										.padding(16.dp),
								text = stringResource(id = R.string.Redeem_Safe3_Redeem_Hint),
								fontSize = 14.sp
						)
					}
				}

				Spacer(modifier = Modifier.height(16.dp))
				body_grey(text = stringResource(id = R.string.Redeem_Safe4_Address))

				Spacer(modifier = Modifier.height(6.dp))
				body_bran(text = uiState.safe4address!!)
			}
		}
				ButtonPrimaryYellow(
						modifier = Modifier
								.padding(horizontal = 16.dp, vertical = 8.dp)
								.weight(0.4f)
								.fillMaxWidth()
								.height(40.dp),
						title = stringResource(id = R.string.Redeem_Safe4_Redeem_Button),
						enabled = uiState.canRedeem,
						onClick = {
							viewModel.showConfirmation()
						}
				)
		}
		}
	}
	if (uiState.showConfirmationDialog) {
		RedeemConfirmationDialog(address = viewModel.receiveAddress(),
				onOKClick = {
					viewModel.redeem()
				},
				onCancelClick = {
					viewModel.closeDialog()
				}
		)
	}
}
