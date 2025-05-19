package io.horizontalsystems.bankwallet.modules.safe4.node.safe3

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
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
import io.horizontalsystems.bankwallet.modules.safe4.node.Safe3Text
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection2
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput2
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.TextPreprocessor
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_bran
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper

class RedeemSafe3Fragment(): BaseComposeFragment() {
	@Composable
	override fun GetContent(navController: NavController) {
		val input = navController.getInput<RedeemSafe3Module.Input>()
		val wallet = input?.wallet ?: return

		val viewModel by viewModels<RedeemSafe3ViewModel> { RedeemSafe3Module.Factory(wallet) }
		RedeemSafe3Screen(viewModel = viewModel, navController = navController)
	}
}

@Composable
fun RedeemSafe3Screen(
		viewModel: RedeemSafe3ViewModel,
		navController: NavController
) {
	val uiState = viewModel.uiState
	val currentStep = uiState.step

	val sendResult = viewModel.sendResult
	val view = LocalView.current

	var textState by rememberSaveable("", stateSaver = TextFieldValue.Saver) {
		mutableStateOf(TextFieldValue("", TextRange( 0)))
	}

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
			textState = textState.copy("")
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

		Column(modifier = Modifier
				.padding(16.dp)
				.fillMaxSize()
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
					Row(
						modifier = Modifier
							.padding(16.dp)) {
						Safe3Text(
							stringResource(id = R.string.Redeem_Safe3_Redeem_Desc1),
							stringResource(id = R.string.Redeem_Safe3_Redeem_Desc2),
						)
					}
				}
			}

			Spacer(modifier = Modifier.height(16.dp))
			Row (
					verticalAlignment = Alignment.CenterVertically
			){
				StepView("1",  stringResource(id = R.string.Redeem_Safe3_Verify_Private_Key), currentStep == 1, currentStep > 1)

				Divider(
						modifier = Modifier
								.weight(1f),
						thickness = 1.dp,
						color = ComposeAppTheme.colors.steel10,
				)
				StepView("2",  stringResource(id = R.string.Redeem_Safe3_Query), currentStep == 2, currentStep > 2)
				Divider(
						modifier = Modifier
								.weight(1f),
						thickness = 1.dp,
						color = ComposeAppTheme.colors.steel10,
				)
				StepView("3",  stringResource(id = R.string.Redeem_Safe3_Migration), currentStep == 3, currentStep > 3)
			}

			Spacer(modifier = Modifier.height(16.dp))

			Safe3Text(
				stringResource(id = R.string.Redeem_Safe3_Private_Key1),
				stringResource(id = R.string.Redeem_Safe3_Private_Key2),
				color = ComposeAppTheme.colors.bran
			)
			FormsInput2(
					textState = textState,
					enabled = true,
					pasteEnabled = false,
					qrScannerEnabled = true,
					hint = stringResource(R.string.Redeem_Safe3_Private_Key_Hint)
			) {
				textState = textState.copy(text = it, selection = TextRange(it.length))
				viewModel.onEnterPrivateKey(it)
			}

			if (uiState.privateKeyError) {
				Spacer(modifier = Modifier.height(8.dp))
				Column(
						modifier = Modifier
								.clip(RoundedCornerShape(8.dp))
								.border(1.dp, ComposeAppTheme.colors.yellow50, RoundedCornerShape(8.dp))
								.background(ComposeAppTheme.colors.yellow20)
								.fillMaxWidth()
				) {
					Row(
							verticalAlignment = Alignment.CenterVertically
					) {
						Icon(
								painter = painterResource(id = R.drawable.ic_error_48), contentDescription = null,
								modifier = Modifier
										.padding(start = 16.dp)
										.width(24.dp)
										.height(24.dp))
						Text(
								modifier = Modifier
										.padding(16.dp),
								text = stringResource(id = R.string.Redeem_Safe3_Private_Key_Error))
					}
				}
			}

			if (currentStep >= 2) {
				Spacer(modifier = Modifier.height(16.dp))
				Column(
						modifier = Modifier
								.clip(RoundedCornerShape(8.dp))
								.border(1.dp, ComposeAppTheme.colors.lawrence, RoundedCornerShape(8.dp))
								.background(ComposeAppTheme.colors.lawrence)
								.fillMaxWidth()
								.padding(16.dp)
				) {

					body_grey(text = stringResource(id = R.string.Redeem_Safe3_Account_Balance))

					Spacer(modifier = Modifier.height(6.dp))
					body_bran(text = uiState.safe3Balance,
							textDecoration = if (uiState.existAvailable) TextDecoration.None else TextDecoration.LineThrough)

					Spacer(modifier = Modifier.height(16.dp))
					Row {
						body_grey(text = stringResource(id = R.string.Redeem_Safe3_Lock_Balance))
						Spacer(modifier = Modifier.width(16.dp))
						body_grey(text = stringResource(id = R.string.Redeem_Safe3_Lock_Num, uiState.safe3LockNum))
					}

					Spacer(modifier = Modifier.height(6.dp))
					body_bran(text = uiState.safe3LockBalance,
							textDecoration = if (uiState.existLocked) TextDecoration.None else TextDecoration.LineThrough)
					if (uiState.masterNodeLock != null) {
						Spacer(modifier = Modifier.height(16.dp))
						body_grey(text = stringResource(id = R.string.Redeem_Safe3_Master_Balance))

						Spacer(modifier = Modifier.height(6.dp))
						body_bran(text = uiState.masterNodeLock,
								textDecoration = if (uiState.existLocked) TextDecoration.None else TextDecoration.LineThrough)
					}
				}

				/*Spacer(modifier = Modifier.height(16.dp))
				body_bran(text = stringResource(id = R.string.Redeem_Safe3_Private_Key))
				FormsInput(
						enabled = true,
						pasteEnabled = false,
						qrScannerEnabled = true,
						hint = stringResource(R.string.Redeem_Safe3_Private_Key_Hint),
				) {
					viewModel.onEnterPrivateKey(it)
				}

				if (uiState.privateKeyError) {
					Spacer(modifier = Modifier.height(8.dp))
					Column(
							modifier = Modifier
									.clip(RoundedCornerShape(8.dp))
									.border(1.dp, ComposeAppTheme.colors.yellow50, RoundedCornerShape(8.dp))
									.background(ComposeAppTheme.colors.yellow20)
									.fillMaxWidth()
					) {
						Row(
								verticalAlignment = Alignment.CenterVertically
						) {
							Icon(
									painter = painterResource(id = R.drawable.ic_error_48), contentDescription = null,
									modifier = Modifier
											.padding(start = 16.dp)
											.width(24.dp)
											.height(24.dp))
							Text(
									modifier = Modifier
											.padding(16.dp),
									text = stringResource(id = R.string.Redeem_Safe3_Private_Key_Error))
						}
					}
				}*/
			}

			if (currentStep >= 3) {

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
						Row {
							Safe3Text(
								stringResource(id = R.string.Redeem_Safe3_Redeem_Hint1),
								stringResource(id = R.string.Redeem_Safe3_Redeem_Hint2),
							)
						}
					}
				}

				Spacer(modifier = Modifier.height(16.dp))
				body_grey(text = stringResource(id = R.string.Redeem_Safe4_Address))

				Spacer(modifier = Modifier.height(6.dp))

				CellUniversalLawrenceSection2(uiState.safe4address) { item ->
					AddressCell(item.title, item.address, item.selected) {
						viewModel.onSelectAddress(item.id)
					}
				}

				Spacer(modifier = Modifier.height(8.dp))
				ButtonPrimaryYellow(
						modifier = Modifier
								.padding(16.dp)
								.fillMaxWidth()
								.height(40.dp),
						title = stringResource(id = R.string.Redeem_Safe4_Redeem_Button),
						enabled = uiState.canRedeem,
						onClick = {
							viewModel.redeem()
						}
				)
			}
			
		}
	}
}


@Composable
fun AddressCell(
	title: String,
	subtitle: String,
	checked: Boolean,
	onClick: () -> Unit
) {
	RowUniversal(
		onClick = onClick
	) {
		Column(modifier = Modifier
			.padding(start = 16.dp)
			.weight(1f)) {
			body_leah(
				text = title,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
			)
			Spacer(Modifier.height(1.dp))
			subhead2_grey(
				text = subtitle,
				overflow = TextOverflow.Ellipsis,
			)
		}
		Box(
			modifier = Modifier
				.width(52.dp)
				.fillMaxHeight(),
			contentAlignment = Alignment.Center
		) {
			if (checked) {
				Icon(
					painter = painterResource(R.drawable.ic_checkmark_20),
					tint = ComposeAppTheme.colors.jacob,
					contentDescription = null,
				)
			}
		}
	}
}

@Composable
fun StepView(
		step: String,
		stepName: String,
		enable: Boolean,
		success: Boolean
) {
	val background = if (enable) ComposeAppTheme.colors.issykBlue else ComposeAppTheme.colors.grey50
	val textColor = if (enable) ComposeAppTheme.colors.white else ComposeAppTheme.colors.black50
	Row(
			verticalAlignment = Alignment.CenterVertically
	) {
		Box(
				modifier = Modifier
						.size(30.dp)
						.clip(CircleShape)
//						.border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))
						.background(background),
				contentAlignment = Alignment.Center
				) {
			if (success) {
				Icon(
						painter = painterResource(id = R.drawable.ic_check_20),
						contentDescription = null,
						tint = ComposeAppTheme.colors.remus
				)
			} else {
				Text(
						text = step,
						color = textColor,
						style = ComposeAppTheme.typography.body,
						fontSize = 14.sp,
						textAlign = TextAlign.Center
				)
			}
		}

		Text(
				text = stepName,
				modifier = Modifier
						.padding(start = 8.dp),
				color = ComposeAppTheme.colors.black50,
				style = ComposeAppTheme.typography.body,
				fontSize = 14.sp
		)
	}
}