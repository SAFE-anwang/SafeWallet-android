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
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput2
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.TextPreprocessor
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_bran
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_grey
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper

class GetSafe3TestCoinFragment(): BaseComposeFragment() {
	@Composable
	override fun GetContent(navController: NavController) {
		val input = navController.getInput<RedeemSafe3Module.GetTestCoinInput>()
		val wallet = input?.safe4Wallet

		val viewModel by viewModels<GetSafe3TestCoinViewModel> { RedeemSafe3Module.Factory3(wallet) }
		GetSafe3TestCoinScreen(viewModel = viewModel, navController = navController)
	}
}

@Composable
fun GetSafe3TestCoinScreen(
		viewModel: GetSafe3TestCoinViewModel,
		navController: NavController
) {
	val uiState = viewModel.uiState

	val sendResult = viewModel.sendResult
	val view = LocalView.current

	var textState by rememberSaveable("", stateSaver = TextFieldValue.Saver) {
		mutableStateOf(TextFieldValue(uiState.initAddress, TextRange( 0)))
	}

	when (sendResult) {
		SendResult.Sending -> {
			HudHelper.showInProcessMessage(
					view,
					R.string.Get_Safe3_Test_Coin_Send_Sending,
					SnackbarDuration.INDEFINITE
			)
		}

		SendResult.Sent -> {
			HudHelper.showSuccessMessage(
					view,
					R.string.Get_Safe3_Test_Coin_Send_Success,
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
		AppBar(
				title = stringResource(id = R.string.Get_Safe3_Test_Coin),
				navigationIcon = {
					HsBackButton(onClick = { navController.popBackStack() })
				}
		)
		Column(modifier = Modifier
				.padding(16.dp)
				.fillMaxSize()
				.verticalScroll(rememberScrollState())) {

			body_bran(text = stringResource(id = R.string.Safe3_Test_Coin_Address))
			FormsInput2(
					textState = textState,
					enabled = true,
					pasteEnabled = false,
					qrScannerEnabled = true,
					hint = stringResource(R.string.Safe3_Test_Coin_Address_Hint)
			) {
				textState = textState.copy(text = it, selection = TextRange(it.length))
				viewModel.enterAddress(it)
			}

			if (uiState.success) {
				Spacer(modifier = Modifier.height(16.dp))
				Column(
						modifier = Modifier
								.clip(RoundedCornerShape(8.dp))
								.border(1.dp, ComposeAppTheme.colors.lawrence, RoundedCornerShape(8.dp))
								.background(ComposeAppTheme.colors.lawrence)
								.fillMaxWidth()
								.padding(16.dp)
				) {
					uiState.amount?.let {
						body_grey(text = stringResource(id = R.string.Get_Safe3_Test_Coin_Amount))
						Spacer(modifier = Modifier.height(6.dp))
						body_bran(text = uiState.amount)
					}

					uiState.transactionHash?.let {
						Divider(
								modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
								thickness = 1.dp,
								color = ComposeAppTheme.colors.steel10,
						)
						body_grey(text = stringResource(id = R.string.Get_Safe3_Test_Coin_Transaction_Hash))
						Spacer(modifier = Modifier.height(6.dp))
						body_bran(text = uiState.transactionHash)
					}

					uiState.dateTimestamp?.let {
						Divider(
								modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
								thickness = 1.dp,
								color = ComposeAppTheme.colors.steel10,
						)
						body_grey(text = stringResource(id = R.string.Get_Safe3_Test_Coin_DateTimestamp))
						Spacer(modifier = Modifier.height(6.dp))
						body_bran(text = uiState.dateTimestamp)
					}

					uiState.from?.let {
						Divider(
								modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
								thickness = 1.dp,
								color = ComposeAppTheme.colors.steel10,
						)
						body_grey(text = stringResource(id = R.string.Get_Safe3_Test_Coin_From))
						Spacer(modifier = Modifier.height(6.dp))
						body_bran(text = uiState.from)
					}

					uiState.from?.let {
						Divider(
								modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
								thickness = 1.dp,
								color = ComposeAppTheme.colors.steel10,
						)
						body_grey(text = stringResource(id = R.string.Get_Safe3_Test_Coin_From))
						Spacer(modifier = Modifier.height(6.dp))
						body_bran(text = uiState.from)
					}

					uiState.nonce?.let {
						Divider(
								modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
								thickness = 1.dp,
								color = ComposeAppTheme.colors.steel10,
						)
						body_grey(text = stringResource(id = R.string.Get_Safe3_Test_Coin_Nonce))
						Spacer(modifier = Modifier.height(6.dp))
						body_bran(text = uiState.nonce)
					}

				}
			}

			uiState.error?.let {
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
								text = it)
					}
				}
			}

			Spacer(modifier = Modifier.height(8.dp))
			ButtonPrimaryYellow(
					modifier = Modifier
							.padding(16.dp)
							.fillMaxWidth()
							.height(40.dp),
					title = stringResource(id = R.string.Get_Safe3_Test_Coin_Button),
					enabled = uiState.enable,
					onClick = {
						viewModel.getTestCoin(textState.text)
					}
			)
		}
	}
}
