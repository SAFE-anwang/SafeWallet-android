package io.horizontalsystems.bankwallet.modules.safe4.node.proposal.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator.getString
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.safe4.node.confirmation.SafeFourConfirmationModule
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.SafeFourProposalModule
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import java.math.BigDecimal
import java.text.SimpleDateFormat

@Composable
fun SafeFourCreateProposalScreen(
		viewModel: SafeFourCreateProposalViewModel,
		navController: NavController,
		isSuper:Boolean
) {
	val wallet = viewModel.wallet
	val uiState = viewModel.uiState
	val balance = uiState.balance
	val processEnable = uiState.canSend

	var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
		mutableStateOf(TextFieldValue(""))
	}

	val options = listOf(
			stringResource(id = R.string.Safe_Four_Proposal_Issuance_Method_One),
			stringResource(id = R.string.Safe_Four_Proposal_Issuance_Method_Instalments)
			)
	var selectedMethod by remember{ mutableStateOf(0) }
	var nameErrorState by remember{ mutableStateOf(false) }
	var descErrorState by remember{ mutableStateOf(false) }

	Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
		AppBar(
				title = stringResource(id = R.string.Safe_Four_Proposal_Create),
				navigationIcon = {
					HsBackButton(onClick = { navController.popBackStack() })
				}
		)
		Column(modifier = Modifier
				.padding(horizontal = 16.dp)
				.verticalScroll(rememberScrollState())) {
			Spacer(modifier = Modifier.height(6.dp))

			body_bran( text = stringResource(id = R.string.Safe_Four_Proposal_Create_Title))

			FormsInput(
					enabled = true,
					pasteEnabled = false,
					hint = stringResource(R.string.Safe_Four_Proposal_Create_Title_Hint),
			) {
				viewModel.onEnterTitle(it)
				nameErrorState = it.length < 8
			}

			if (nameErrorState) {
				Text(
						modifier = Modifier.padding(start = 16.dp),
						text = stringResource(id = R.string.Safe_Four_Register_Mode_Length_Error, 8),
						color = ComposeAppTheme.colors.redD,
						style = ComposeAppTheme.typography.caption,
						maxLines = 1,
				)
			}

			Spacer(modifier = Modifier.height(12.dp))

			body_bran( text = stringResource(id = R.string.Safe_Four_Proposal_Create_Desc))

			FormsInput(
					enabled = true,
					pasteEnabled = false,
					hint = stringResource(R.string.Safe_Four_Proposal_Create_Desc_Hint),
			) {
				viewModel.onEnterDescription(it)
				descErrorState = it.length < 8
			}

			if (descErrorState) {
				Text(
						modifier = Modifier.padding(start = 16.dp),
						text = stringResource(id = R.string.Safe_Four_Register_Mode_Length_Error, 8),
						color = ComposeAppTheme.colors.redD,
						style = ComposeAppTheme.typography.caption,
						maxLines = 1,
				)
			}

			Spacer(modifier = Modifier.height(12.dp))

			Column(
					modifier = Modifier
							.fillMaxWidth()
							.clip(RoundedCornerShape(8.dp))
							.border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))
							.background(ComposeAppTheme.colors.lawrence)
			) {
				Row(
						modifier = Modifier
								.padding(16.dp),
				){
					Text(
							text = stringResource(id = R.string.Safe_Four_Proposal_Create_Balance),
							style = ComposeAppTheme.typography.body,
							color = ComposeAppTheme.colors.grey,
							overflow = TextOverflow.Ellipsis,
							maxLines = 1,
					)
					Spacer(Modifier.weight(1f))
					Text(
							text = balance,
							color = ComposeAppTheme.colors.grey,
							style = ComposeAppTheme.typography.body,
							maxLines = 1,
					)
				}
			}

			Spacer(modifier = Modifier.height(12.dp))
			body_bran( text = stringResource(id = R.string.Safe_Four_Proposal_Create_Apply))

			FormsInput(
					enabled = true,
					pasteEnabled = false,
					hint = stringResource(R.string.Safe_Four_Proposal_Create_Apply_Hint),
					singleLine = true,
					keyboardOptions = KeyboardOptions(
							keyboardType = KeyboardType.Decimal
					),
			) {
				if (viewModel.isValid(it)) {
					viewModel.onEnterAmount(it)
				}
			}

			Spacer(modifier = Modifier.height(12.dp))
			body_bran( text = stringResource(id = R.string.Safe_Four_Proposal_Info_Issuance_Method_2))
			Column(
					modifier = Modifier
							.fillMaxWidth()
							.clip(RoundedCornerShape(8.dp))
							.border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))
							.background(ComposeAppTheme.colors.lawrence)
			) {
				Row {
					options.forEachIndexed { index, option ->
						Row(modifier = Modifier
								.fillMaxWidth()
								.padding(5.dp)
								.weight(1f),
								verticalAlignment = Alignment.CenterVertically
						) {
							RadioButton(
									selected = index == selectedMethod,
									onClick = {
										if (selectedMethod != index) {
											viewModel.clearTime()
										}
										selectedMethod = index
										viewModel.onSetPayTimes(index + 1)
									}
							)
							Text(
									text = option,
									style = ComposeAppTheme.typography.body,
									color = ComposeAppTheme.colors.grey,
									overflow = TextOverflow.Ellipsis,
									maxLines = 1,
							)
						}
					}
				}
			}

			Spacer(modifier = Modifier.height(12.dp))

			body_bran( text = stringResource(id = R.string.Safe_Four_Proposal_Create_Time))
			Column(
					modifier = Modifier
							.fillMaxWidth()
							.clip(RoundedCornerShape(8.dp))
							.border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))
							.background(ComposeAppTheme.colors.lawrence)
							.padding(16.dp),
			) {
				if (selectedMethod == 0) {
					SingleButtonDatePickerView(
							value = stringResource(id = R.string.Safe_Four_Proposal_Create_Time_Hint),
							onValueChange = {
								viewModel.onEnterStartTime(it)
								viewModel.onEnterEndTime(it)
							}
					)
				} else {

					TwoButtonDatePickerView(
							startDate = stringResource(id = R.string.Safe_Four_Proposal_Create_Time_Start),
							endDate = stringResource(id = R.string.Safe_Four_Proposal_Create_Time_End),
							onStartChange = {
								viewModel.onEnterStartTime(it)
							},
							onEndChange = {
								viewModel.onEnterEndTime(it)
							}
					)

					Spacer(modifier = Modifier.height(12.dp))
					body_bran( text = stringResource(id = R.string.Safe_Four_Proposal_Create_Instalments_Num))

					FormsInput(
							enabled = true,
							pasteEnabled = false,
							initial = "2",
							hint = "",
							singleLine = true,
							keyboardOptions = KeyboardOptions(
									keyboardType = KeyboardType.Decimal
							),
					) {
						if (viewModel.isValidTimes(it)) {
							viewModel.onSetPayTimes(it.toInt())
						}
					}
				}

			}

			Row {
				ButtonPrimaryYellow(
						modifier = Modifier
								.weight(1f)
								.padding(16.dp)
								.height(40.dp),
						title = stringResource(R.string.Safe_Four_Register_Node_Create),
						onClick = {
							navController.slideFromRight(
									R.id.createProposalConfirmationFragment,
									SafeFourProposalModule.CreateProposalInput(wallet, viewModel.getCreateProposalData())
							)
						},
						enabled = processEnable
				)
			}
		}
	}
}
