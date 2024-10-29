package io.horizontalsystems.bankwallet.modules.safe4.node.addlockday

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourModule
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay

class AddLockDayFragment: BaseComposeFragment() {

	@Composable
	override fun GetContent(navController: NavController) {

		val input = navController.getInput<SafeFourModule.AddLockDayInput>()
		val wallet = input?.wallet ?: return
		val nodeId = input?.lockId ?: return
		val viewModel by viewModels<AddLockDayViewModel> { SafeFourModule.FactoryAddLockDay(wallet, nodeId) }

		AddLockDayScreen(
				viewModel,
				navController
		)
	}

}


@Composable
fun AddLockDayScreen(
		viewModel: AddLockDayViewModel,
		navController: NavController
) {
	val uiState = viewModel.uiState

	val sendResult = viewModel.sendResult
	val view = LocalView.current

	var day by remember{ mutableStateOf(360) }

	when (sendResult) {
		SendResult.Sending -> {
			HudHelper.showInProcessMessage(
					view,
					R.string.Send_Sending,
					SnackbarDuration.INDEFINITE
			)
		}

		SendResult.Sent -> {
			HudHelper.showSuccessMessage(
					view,
					R.string.Send_Success,
					SnackbarDuration.LONG
			)
		}

		is SendResult.Failed -> {
			HudHelper.showErrorMessage(view, sendResult.caution.getString())
		}

		null -> Unit
	}

	LaunchedEffect(sendResult) {
		if (sendResult == SendResult.Sent) {
			delay(1200)
			navController.popBackStack(R.id.addLockDayFragment, true)
		}
	}

	Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
		AppBar(
				title = stringResource(id = R.string.Safe_Four_Node_Add_Lock_Day),
				navigationIcon = {
					HsBackButton(onClick = { navController.popBackStack() })
				}
		)

		Column(modifier = Modifier
				.padding(horizontal = 16.dp)
				.verticalScroll(rememberScrollState())) {

			Text(
					text = stringResource(id = R.string.Safe_Four_Node_Add_Lock_Day_Period),
					style = ComposeAppTheme.typography.body,
					color = ComposeAppTheme.colors.grey,
					overflow = TextOverflow.Ellipsis,
					maxLines = 1,
			)
			Spacer(modifier = Modifier.height(6.dp))
			Column(
					modifier = Modifier
							.fillMaxWidth()
							.clip(RoundedCornerShape(8.dp))
							.border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))
							.background(ComposeAppTheme.colors.lawrence),
					horizontalAlignment = Alignment.CenterHorizontally
			) {

				Row(
						modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
						verticalAlignment = Alignment.CenterVertically
				) {
					Icon(
							painter = painterResource(id = R.drawable.ic_reduce_circle_24), contentDescription = null,
							tint = ComposeAppTheme.colors.grey,
							modifier = Modifier
									.padding(start = 16.dp)
									.width(32.dp)
									.height(32.dp)
									.clickable(enabled = day > 360) {
										if (day.toInt() > 360) {
											day -= 360
											viewModel.onEnterDay(day)
										}
									})

					Spacer(modifier = Modifier.width(16.dp))

					Text(
							text = stringResource(id = R.string.Safe_Four_Node_Add_Lock_Day_Text, day.toInt()),
							style = ComposeAppTheme.typography.body,
							color = ComposeAppTheme.colors.bran,
							overflow = TextOverflow.Ellipsis,
							maxLines = 1,
					)

					Spacer(modifier = Modifier.width(16.dp))

					Icon(
							painter = painterResource(id = R.drawable.ic_add_circle_24), contentDescription = null,
							tint = ComposeAppTheme.colors.grey,
							modifier = Modifier
									.padding(start = 16.dp)
									.width(32.dp)
									.height(32.dp)
									.clickable(enabled = day.toInt() < uiState.maxLockDay) {
										if (day.toInt() < uiState.maxLockDay) {
											day += 360
											viewModel.onEnterDay(day)
										}
									})

				}

			}
			/*FormsInput(
					enabled = uiState.addEnable,
					pasteEnabled = false,
					initial = "360",
					hint = stringResource(id = R.string.Safe_Four_Node_Add_Lock_Day_Input_Hint),
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
			) {
				viewModel.onEnterDay(it)
			}*/

			uiState.error?.let{
				Spacer(modifier = Modifier.height(6.dp))
				Text(
						modifier = Modifier.padding(start = 16.dp),
						text = stringResource(id = it),
						color = ComposeAppTheme.colors.redD,
						style = ComposeAppTheme.typography.caption,
						maxLines = 1,
				)
			}

			uiState.errorMaxDay?.let{
				Spacer(modifier = Modifier.height(6.dp))
				Text(
						modifier = Modifier.padding(start = 16.dp),
						text = stringResource(id = it, uiState.maxLockDay),
						color = ComposeAppTheme.colors.redD,
						style = ComposeAppTheme.typography.caption,
						maxLines = 1,
				)
			}

			Spacer(modifier = Modifier.height(16.dp))

			ButtonPrimaryYellow(
					modifier = Modifier
							.fillMaxWidth()
							.height(40.dp),
					title = stringResource(R.string.Safe_Four_Node_Add_Lock_Day_Button),
					enabled = uiState.addEnable,
					onClick = {
						viewModel.showConfirmation()
					}
			)
		}
	}

	if (uiState.showConfirmationDialog) {
		AddLockDayConfirmationDialog(day,
				onOKClick = {
					viewModel.addLockDay()
				},
				onCancelClick = {
					viewModel.closeDialog()
				}
		)
	}
}