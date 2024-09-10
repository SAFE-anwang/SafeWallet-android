package io.horizontalsystems.bankwallet.modules.safe4.node.supernode

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.RadioButton
import androidx.compose.material.RangeSlider
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator.getString
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.amount.HSAmountInput
import io.horizontalsystems.bankwallet.modules.availablebalance.AvailableBalance
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory.Master_Node_Create_Amount
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeCovertFactory.Super_Node_Create_Amount
import io.horizontalsystems.bankwallet.modules.safe4.node.confirmation.SafeFourConfirmationModule
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah

@Composable
fun SafeFourCreateNodeScreen(
		viewModel: SafeFourCreateNodeViewModel,
		navController: NavController,
		isSuper:Boolean
) {
	val wallet = viewModel.wallet
	val predefinedAddress = null
	val uiState = viewModel.uiState
	val addressError = viewModel.uiState.addressError
	val lockValue = uiState.lockAmount

	val availableBalance = uiState.availableBalance
	val proceedEnabled = uiState.canBeSend

	val title = if (isSuper) R.string.Safe_Four_Register_Super_Node else R.string.Safe_Four_Register_Master_Node
	val options = listOf(
			stringResource(id = R.string.Safe_Four_Register_Mode_Stand_Alone),
			stringResource(id = R.string.Safe_Four_Register_Mode_Crowd_Funding)
			)
	var selectedOption by remember{ mutableStateOf(0) }
	var nameErrorState by remember{ mutableStateOf(false) }
	var descErrorState by remember{ mutableStateOf(false) }

	Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
		AppBar(
				title = stringResource(id = title),
				navigationIcon = {
					HsBackButton(onClick = { navController.popBackStack() })
				}
		)
		Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
			body_leah(
					modifier = Modifier.padding(start = 16.dp),
					text = stringResource(R.string.Safe_Four_Register_Node_Create_Mode),
					maxLines = 1,
			)
			Spacer(modifier = Modifier.height(4.dp))
			Column(
					modifier = Modifier
							.fillMaxWidth()
							.padding(start = 16.dp, end = 16.dp)
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
								selected = index == selectedOption,
								onClick = {
									selectedOption = index
									viewModel.onSelectType(index)
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
			AvailableBalance(
					coinCode = wallet.coin.code,
					coinDecimal = viewModel.coinMaxAllowedDecimals,
					fiatDecimal = viewModel.fiatMaxAllowedDecimals,
					availableBalance = availableBalance,
					amountInputType = AmountInputType.COIN,
					rate = viewModel.coinRate
			)
			Column(
					modifier = Modifier
							.padding(16.dp)
							.fillMaxWidth()
							.clip(RoundedCornerShape(8.dp))
							.border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))
							.background(ComposeAppTheme.colors.lawrence)
			) {
				Row(
					modifier = Modifier
								.padding(16.dp)) {
					Text(
							text = stringResource(id = R.string.Safe_Four_Register_Lock),
							style = ComposeAppTheme.typography.body,
							color = ComposeAppTheme.colors.grey,
							overflow = TextOverflow.Ellipsis,
							maxLines = 1,
					)
					Spacer(Modifier.weight(1f))
					Text(
							text = "$lockValue",
							color = ComposeAppTheme.colors.grey,
							style = ComposeAppTheme.typography.body,
							maxLines = 1,
					)
				}
			}

			Spacer(modifier = Modifier.height(12.dp))
			val addressHint = if (isSuper) {
				R.string.Safe_Four_Register_Mode_Super_Node_Address
			} else {
				R.string.Safe_Four_Register_Mode_Master_Node_Address
			}
			Text(
					modifier = Modifier.padding(start = 16.dp),
					text = stringResource(id = addressHint),
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
			if (uiState.existNode || uiState.isFounder) {
				Text(
						modifier = Modifier.padding(start = 16.dp),
						text = stringResource(
								id = if (isSuper) R.string.Safe_Four_Register_Mode_Exists_Super_Node
									else R.string.Safe_Four_Register_Mode_Exists_Master_Node
						),
						color = ComposeAppTheme.colors.redD,
						style = ComposeAppTheme.typography.caption,
						maxLines = 1,
				)
			}
			if (uiState.isInputCurrentWalletAddress) {
				Text(
						modifier = Modifier.padding(start = 16.dp),
						text = stringResource(
								id =  R.string.Safe_Four_Register_Mode_Dont_User_Current_Wallet_Address
						),
						color = ComposeAppTheme.colors.redD,
						style = ComposeAppTheme.typography.caption,
						maxLines = 1,
				)
			}
			if (isSuper) {
				Spacer(modifier = Modifier.height(12.dp))

				body_bran(modifier = Modifier.padding(start = 16.dp),
						text = stringResource(id = R.string.Safe_Four_Register_Mode_Name))

				FormsInput(
						modifier = Modifier.padding(horizontal = 16.dp),
						enabled = true,
						pasteEnabled = false,
						hint = "",
				) {
					viewModel.onEnterNodeName(it)
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
			}
			Spacer(modifier = Modifier.height(12.dp))

			body_bran(modifier = Modifier.padding(start = 16.dp), text = stringResource(id = R.string.Safe_Four_Register_ENODE))
			Spacer(modifier = Modifier.height(6.dp))
			FormsInput(
					modifier = Modifier.padding(horizontal = 16.dp),
					enabled = true,
					pasteEnabled = true,
					hint = "",
					qrScannerEnabled = true,
			) {
				viewModel.onEnterENode(it)
			}
			if (uiState.existEnode) {
				Text(
						modifier = Modifier.padding(start = 16.dp),
						text = stringResource(id = R.string.Safe_Four_Register_Mode_Exists_Enode),
						color = ComposeAppTheme.colors.redD,
						style = ComposeAppTheme.typography.caption,
						maxLines = 1,
				)
			}

			body_grey(modifier = Modifier.padding(start = 16.dp), text = stringResource(id = R.string.Safe_Four_Register_ENODE_Hint))

			Spacer(modifier = Modifier.height(12.dp))

			body_bran(modifier = Modifier.padding(start = 16.dp), text = stringResource(id = R.string.Safe_Four_Register_Introduction))
			FormsInput(
					modifier = Modifier.padding(horizontal = 16.dp),
					enabled = true,
					pasteEnabled = false,
					hint = "",
			) {
				viewModel.onEnterIntroduction(it)
				descErrorState = it.length < 12
			}

			if (descErrorState) {
				Text(
						modifier = Modifier.padding(start = 16.dp),
						text = stringResource(id = R.string.Safe_Four_Register_Mode_Length_Error, 12),
						color = ComposeAppTheme.colors.redD,
						style = ComposeAppTheme.typography.caption,
						maxLines = 1,
				)
			}

			if (isSuper || selectedOption == 1) {
				Spacer(modifier = Modifier.height(12.dp))
				body_bran(
						modifier = Modifier.padding(start = 16.dp),
						text = stringResource(id = R.string.Safe_Four_Register_Reward))

				Spacer(modifier = Modifier.height(12.dp))

				if (isSuper) {
					RangeSliderScreen(
					) { t1, t2, t3 ->
						viewModel.onEnterIncentive(t1, t2, t3)
					}
				} else {
					SliderScreen(
							45f,
							1,
							50,
							enabled = selectedOption == 1,
							modifier = Modifier,
							0f..100f
					) {
						viewModel.onEnterIncentive(100 - it, it)
					}
				}
			}

			Spacer(modifier = Modifier.height(12.dp))

			Row {
				ButtonPrimaryYellow(
						modifier = Modifier
								.weight(1f)
								.padding(16.dp)
								.height(40.dp),
						title = stringResource(R.string.Safe_Four_Register_Node_Create),
						onClick = {
							navController.slideFromRight(
									R.id.createNodeConfirmationFragment,
									SafeFourConfirmationModule.Input(
											isSuper,
											viewModel.getCreateNodeData(),
											wallet,
											R.id.nodeListFragment,
											0)
							)
						},
						enabled = proceedEnabled
				)
			}
		}
	}
}


@Composable
fun SliderScreen(
		initValue: Float,
		min: Int,
		max: Int,
		enabled: Boolean = true,
		modifier: Modifier = Modifier,
		valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
		onValueChange: (Int) -> Unit) {
	var sliderPosition by remember { mutableFloatStateOf(initValue) }

	var progressPartner by remember { mutableStateOf(50) }
	var progressCreator by remember { mutableStateOf(50) }
	Column(modifier = modifier) {
		Slider(
				modifier = Modifier
						.fillMaxWidth()
						.padding(start = 16.dp, end = 16.dp),
				value = sliderPosition,
				enabled = enabled,
				onValueChange = {
					sliderPosition = it
				},
				onValueChangeFinished = {
					val temp = sliderPosition.toInt()
					if (temp < min) {
						sliderPosition = min.toFloat()
					}
					if (temp > max) {
						sliderPosition = max.toFloat()
					}
					progressCreator = sliderPosition.toInt()
					progressPartner = 100 - sliderPosition.toInt()
					onValueChange.invoke(sliderPosition.toInt())
				},
//				steps = 1,
				valueRange = valueRange
		)
		Row{
			Column(
					modifier = Modifier
							.fillMaxWidth()
							.padding(start = 16.dp)
							.weight(1f)
			) {
				body_bran(
						text = stringResource(id = R.string.Safe_Four_Register_Creator))
				Text(
						text = "${progressCreator.toString()}%"
				)
			}

			Column(
					modifier = Modifier
							.fillMaxWidth()
							.padding(end = 16.dp)
							.weight(1f)
					,
					horizontalAlignment = Alignment.End
			) {
				body_bran(text = stringResource(id = R.string.Safe_Four_Register_Partner))

				Text(text = "${progressPartner.toString()}%")
			}
		}
	}
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RangeSliderScreen(
		initValue: ClosedFloatingPointRange<Float> = 45f..55f,
		enabled: Boolean = true,
		modifier: Modifier = Modifier,
		valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
		onValueChange: (Int, Int, Int) -> Unit) {

	var sliderPosition by remember { mutableStateOf(initValue) }
	var historyStart by remember { mutableStateOf(initValue.start.toInt()) }
	var historyEnd by remember { mutableStateOf(initValue.endInclusive.toInt()) }
	var progressPartner by remember { mutableStateOf(45) }
	var progressCreator by remember { mutableStateOf(10) }
	var progressVoter by remember { mutableStateOf(45) }
	Column(modifier = modifier) {
			RangeSlider(
					modifier = Modifier
							.fillMaxWidth()
							.padding(start = 16.dp, end = 16.dp),
					value = sliderPosition,
					enabled = enabled,
//					steps = 1,
					onValueChange = { range -> sliderPosition = range },
					valueRange = valueRange,
					onValueChangeFinished = {
						// launch some business logic update with the state you hold
						// viewModel.updateSelectedSliderValue(sliderPosition)
						var start = sliderPosition.start.toInt()
						var end = sliderPosition.endInclusive.toInt()
						var startOffset = 50 - historyStart
						var endOffset = 60 - historyEnd
						var startMin = 50 - endOffset
						var endMax = 60 - startOffset


						if (start < startMin) {
							start = startMin
						}
						if (start > 50) {
							start = 50
						}
						if (end < 50) {
							end = 50
						}
						if (end > endMax) {
							end = endMax
						}

						if (end - start > 10) {
							if (end > 50) end -= end - start - 10
						}
						historyStart = start
						historyEnd = end
						progressPartner = start
						progressVoter = 100 - end
						progressCreator = 100 - (progressPartner + progressVoter)
						sliderPosition = start.toFloat()..end.toFloat()
						onValueChange.invoke(
								progressPartner,
								progressCreator,
								progressVoter
						)
					},
			)
			Row{
				Column(
						modifier = Modifier
								.fillMaxWidth()
								.padding(start = 16.dp)
								.weight(1f)
				) {
					body_bran(
							text = stringResource(id = R.string.Safe_Four_Register_Partner))
					Text(
							text = "${progressPartner.toString()}%"
					)
				}

				Column(
						modifier = Modifier
								.fillMaxWidth()
								.weight(1f)
						,
						horizontalAlignment = Alignment.CenterHorizontally
				) {
					body_bran(text = stringResource(id = R.string.Safe_Four_Register_Creator))

					Text(text = "${progressCreator.toString()}%")
				}

				Column(
						modifier = Modifier
								.fillMaxWidth()
								.padding(end = 16.dp)
								.weight(1f),
						horizontalAlignment = Alignment.End
				) {
						body_bran(text = stringResource(id = R.string.Safe_Four_Register_Voters))
						Text(text = "${progressVoter.toString()}%")
				}
			}
	}
}
