package io.horizontalsystems.bankwallet.modules.safe4.node

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.SectionItemPosition
import io.horizontalsystems.bankwallet.ui.compose.components.SectionUniversalItem
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.sectionItemBorder

@Composable
fun SafeFourCreateNodeScreen(
		viewModel: SafeFourCreateNodeViewModel,
		navController: NavController
) {
	val wallet = viewModel.wallet
	val predefinedAddress = viewModel.predefinedAddress
	val uiState = viewModel.uiState
	val addressError = viewModel.uiState.addressError
	var nodeName by remember("superNodeName") {
		mutableStateOf( "")
	}

	var eNode by remember("eNode") {
		mutableStateOf( "")
	}

	var introduction by remember("Introduction") {
		mutableStateOf( "")
	}

	val options = listOf(
			stringResource(id = R.string.Safe_Four_Register_Mode_Stand_Alone),
			stringResource(id = R.string.Safe_Four_Register_Mode_Crowd_Funding)
			)
	var selectedOption by remember{ mutableStateOf(options[0]) }
	Scaffold(
			backgroundColor = ComposeAppTheme.colors.tyler,
			topBar = {
				AppBar(
						title = uiState.title,
						navigationIcon = {
							HsBackButton(onClick = { navController.popBackStack() })
						}
				)
			}
	) { paddingValues ->
		Box(modifier = Modifier.padding(horizontal = 16.dp)) {
			body_leah(
					modifier = Modifier.padding(end = 16.dp),
					text = stringResource(R.string.Safe_Four_Register_Super_Node),
					maxLines = 1,
			)
			options.forEach {  option->
				Row(modifier = Modifier
						.fillMaxWidth()
						.padding(10.dp),
						verticalAlignment = Alignment.CenterVertically) {
					RadioButton(selected = option == selectedOption, onClick = { selectedOption = option})
					Text(
							text = option,
							style = ComposeAppTheme.typography.body,
							color = ComposeAppTheme.colors.grey,
							overflow = TextOverflow.Ellipsis,
							maxLines = 1,
					)
				}
			}
			Column(
					modifier = Modifier
							.padding(end = 16.dp)
							.alpha(1f)
			) {
				Row {
					Text(
							text = stringResource(id = R.string.Safe_Four_Register_Lock),
							style = ComposeAppTheme.typography.body,
							color = ComposeAppTheme.colors.grey,
							overflow = TextOverflow.Ellipsis,
							maxLines = 1,
					)
					Spacer(Modifier.weight(1f))
					Text(
							text = "5,000 SAFE",
							color = ComposeAppTheme.colors.grey,
							style = ComposeAppTheme.typography.body,
							maxLines = 1,
					)
				}
				Row {

					Text(
							text = stringResource(id = R.string.Safe_Four_Register_Balance),
							color = ComposeAppTheme.colors.grey,
							style = ComposeAppTheme.typography.body,
							maxLines = 1,
					)
					Text(
							text = "",
							color = ComposeAppTheme.colors.grey,
							style = ComposeAppTheme.typography.body,
							maxLines = 1,
					)
				}
			}

			Spacer(modifier = Modifier.height(12.dp))

			Text(
					text = stringResource(id = R.string.Safe_Four_Register_Mode_Super_Node_Address),
					color = ComposeAppTheme.colors.grey,
					style = ComposeAppTheme.typography.body,
					maxLines = 1,
			)

			Spacer(modifier = Modifier.height(12.dp))
			HSAddressInput(
					modifier = Modifier.padding(horizontal = 16.dp),
					initial = predefinedAddress?.let { Address(it) },
					tokenQuery = wallet.token.tokenQuery,
					coinCode = wallet.coin.code,
					error = addressError,
					navController = navController
			) {
				viewModel.onEnterAddress(it)
			}

			Spacer(modifier = Modifier.height(12.dp))

			Row {
				body_grey(text = stringResource(id = R.string.Safe_Four_Register_Mode_Name))
				BasicTextField(
						modifier = Modifier.fillMaxWidth(),
						value = nodeName,
						onValueChange = {
							nodeName = it
							viewModel.onEnterNodeName(it)
						},
						enabled = true,
						textStyle = ColoredTextStyle(
								color = ComposeAppTheme.colors.grey, textStyle = ComposeAppTheme.typography.body
						),
						singleLine = true,
						keyboardOptions = KeyboardOptions(
								keyboardType = KeyboardType.Decimal
						),
						cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
						decorationBox = { innerTextField ->
							if (nodeName.isEmpty()) {
								body_grey(text = "0")
							}
							innerTextField()
						},
				)
			}

			Spacer(modifier = Modifier.height(12.dp))

			Row {
				body_grey(text = stringResource(id = R.string.Safe_Four_Register_ENODE))
				Spacer(modifier = Modifier.height(6.dp))
				body_bran(text = stringResource(id = R.string.Safe_Four_Register_ENODE_Hint))
				BasicTextField(
						modifier = Modifier.fillMaxWidth(),
						value = eNode,
						onValueChange = {
							eNode = it
							viewModel.onEnterNodeName(it)
						},
						enabled = true,
						textStyle = ColoredTextStyle(
								color = ComposeAppTheme.colors.grey, textStyle = ComposeAppTheme.typography.body
						),
						singleLine = true,
						keyboardOptions = KeyboardOptions(
								keyboardType = KeyboardType.Decimal
						),
						cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
						decorationBox = { innerTextField ->
							if (nodeName.isEmpty()) {
								body_grey(text = "0")
							}
							innerTextField()
						},
				)
			}

			Spacer(modifier = Modifier.height(12.dp))

			Row {
				body_grey(text = stringResource(id = R.string.Safe_Four_Register_Introduction))
				BasicTextField(
						modifier = Modifier.fillMaxWidth(),
						value = introduction,
						onValueChange = {
							introduction = it
							viewModel.onEnterIntroduction(it)
						},
						enabled = true,
						textStyle = ColoredTextStyle(
								color = ComposeAppTheme.colors.grey, textStyle = ComposeAppTheme.typography.body
						),
						singleLine = true,
						keyboardOptions = KeyboardOptions(
								keyboardType = KeyboardType.Decimal
						),
						cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
						decorationBox = { innerTextField ->
							if (nodeName.isEmpty()) {
								body_grey(text = "0")
							}
							innerTextField()
						},
				)
			}

			Spacer(modifier = Modifier.height(12.dp))

			Row {
				body_grey(text = stringResource(id = R.string.Safe_Four_Register_Reward))
			}
			Spacer(modifier = Modifier.height(12.dp))
			Row {
				body_grey(text = stringResource(id = R.string.Safe_Four_Register_Partner))
				Spacer(Modifier.weight(1f))
				body_grey(text = "45%")
			}

			Spacer(modifier = Modifier.height(12.dp))
			Row {
				body_grey(text = stringResource(id = R.string.Safe_Four_Register_Creator))
				Spacer(Modifier.weight(1f))
				body_grey(text = "10%")
			}

			Spacer(modifier = Modifier.height(12.dp))
			Row {
				body_grey(text = stringResource(id = R.string.Safe_Four_Register_Voters))
				Spacer(Modifier.weight(1f))
				body_grey(text = "10%")
			}

			Spacer(modifier = Modifier.height(12.dp))

			Row {
				ButtonPrimaryYellow(
						modifier = Modifier
								.weight(1f)
								.height(20.dp),
						title = stringResource(R.string.Safe_Four_Vote),
						onClick = {

						}
				)
			}
		}
	}
}
