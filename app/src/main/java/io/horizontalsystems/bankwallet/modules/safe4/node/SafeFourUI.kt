package io.horizontalsystems.bankwallet.modules.safe4.node

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey50
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey50
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey

@Composable
fun HintView(
		textId: Int
) {
	Column(
			modifier = Modifier
					.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
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
							.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
					fontSize = 14.sp,
					text = stringResource(id = textId),
					style = ComposeAppTheme.typography.caption)
		}
	}
}

@Composable
fun SearchBar(
	searchHintText: String = "",
	focusRequester: FocusRequester = remember { FocusRequester() },
	onClose: () -> Unit,
	onSearchTextChanged: (String) -> Unit = {},
) {

	val keyboardController = LocalSoftwareKeyboardController.current
	var searchText by remember { mutableStateOf("") }
	var showClearButton by remember { mutableStateOf(true) }
	Column(
			modifier = Modifier
					.padding(horizontal = 16.dp)
					.clip(RoundedCornerShape(12.dp))
					.wrapContentHeight()
					.background(ComposeAppTheme.colors.lawrence)
	) {
		CustomOutlinedTextField(
				modifier = Modifier
						.fillMaxWidth()
						.focusRequester(focusRequester),
				value = searchText,
				onValueChange = {
					searchText = it
					onSearchTextChanged.invoke(it)
				},
				placeholder = {
					subhead1_grey50(
							text = searchHintText,
							maxLines = 1,
							overflow = TextOverflow.Ellipsis
					)
				},
				textStyle = ComposeAppTheme.typography.subhead1,
				colors = TextFieldDefaults.textFieldColors(
						focusedIndicatorColor = Color.Transparent,
						unfocusedIndicatorColor = Color.Transparent,
						backgroundColor = Color.Transparent,
						cursorColor = ComposeAppTheme.colors.jacob,
						textColor = ComposeAppTheme.colors.leah
				),
				maxLines = 1,
				singleLine = true,
				keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
				keyboardActions = KeyboardActions(onDone = {
					keyboardController?.hide()
				}),
				trailingIcon = {
					AnimatedVisibility(
							visible = showClearButton,
							enter = fadeIn(),
							exit = fadeOut()
					) {
						HsIconButton(onClick = {
							searchText = ""
							onSearchTextChanged.invoke("")
//							showClearButton = false
						}) {
							val icon = if (searchText == "") R.drawable.ic_search else R.drawable.ic_close
							Icon(
									modifier = Modifier.size(24.dp),
									painter = painterResource(icon),
									contentDescription = stringResource(R.string.Button_Cancel),
									tint = ComposeAppTheme.colors.jacob
							)
						}

					}
				},
		)

		/*LaunchedEffect(Unit) {
			focusRequester.requestFocus()
		}*/
	}
}

@Composable
fun CustomOutlinedTextField(
		value: String,
		onValueChange: (String) -> Unit,
		modifier: Modifier = Modifier,
		enabled: Boolean = true,
		readOnly: Boolean = false,
		textStyle: TextStyle = LocalTextStyle.current,
		label: @Composable (() -> Unit)? = null,
		placeholder: @Composable (() -> Unit)? = null,
		leadingIcon: @Composable (() -> Unit)? = null,
		trailingIcon: @Composable (() -> Unit)? = null,
		isError: Boolean = false,
		visualTransformation: VisualTransformation = VisualTransformation.None,
		keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
		keyboardActions: KeyboardActions = KeyboardActions.Default,
		singleLine: Boolean = false,
		maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
		minLines: Int = 1,
		interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
		shape: Shape = MaterialTheme.shapes.small,
		colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors()
) {
	// If color is not provided via the text style, use content color as a default
	val textColor = textStyle.color.takeOrElse {
		colors.textColor(enabled).value
	}
	val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

	@OptIn(ExperimentalMaterialApi::class)
	(BasicTextField(
				value = value,
				modifier = if (label != null) {
					modifier
							// Merge semantics at the beginning of the modifier chain to ensure padding is
							// considered part of the text field.
							.semantics(mergeDescendants = true) {}
							.padding(top = 8.dp)
				} else {
					modifier
				}
						.background(colors.backgroundColor(enabled).value, shape)
//						.defaultErrorSemantics(isError, getString(Strings.DefaultErrorMessage))
						.defaultMinSize(
								minWidth = TextFieldDefaults.MinWidth,
								minHeight = 40.dp
						),
				onValueChange = onValueChange,
				enabled = enabled,
				readOnly = readOnly,
				textStyle = mergedTextStyle,
				cursorBrush = SolidColor(colors.cursorColor(isError).value),
				visualTransformation = visualTransformation,
				keyboardOptions = keyboardOptions,
				keyboardActions = keyboardActions,
				interactionSource = interactionSource,
				singleLine = singleLine,
				maxLines = maxLines,
				minLines = minLines,
				decorationBox = @Composable { innerTextField ->
					TextFieldDefaults.OutlinedTextFieldDecorationBox(
							value = value,
							visualTransformation = visualTransformation,
							innerTextField = innerTextField,
							placeholder = placeholder,
							label = label,
							leadingIcon = leadingIcon,
							trailingIcon = trailingIcon,
							singleLine = singleLine,
							enabled = enabled,
							isError = isError,
							interactionSource = interactionSource,
							colors = colors,
							border = {
								TextFieldDefaults.BorderBox(
										enabled,
										isError,
										interactionSource,
										colors,
										shape
								)
							}
					)
				}
		))
}



@Composable
fun ListEmptyView2(
		paddingValues: PaddingValues = PaddingValues(),
		text: String,
		@DrawableRes icon: Int
) {
	ScreenMessageWithAction2(
			paddingValues = paddingValues,
			text = text,
			icon = icon
	)
}

@Composable
fun ScreenMessageWithAction2(
		text: String,
		@DrawableRes icon: Int,
		paddingValues: PaddingValues = PaddingValues(),
		actionsComposable: (@Composable () -> Unit)? = null
) {
	Column(
			modifier = Modifier
					.padding(paddingValues)
					.fillMaxSize(),
			verticalArrangement = Arrangement.Center,
			horizontalAlignment = Alignment.CenterHorizontally
	) {
		Box(
				modifier = Modifier
						.size(100.dp)
						.background(
								color = ComposeAppTheme.colors.raina,
								shape = CircleShape
						),
				contentAlignment = Alignment.Center
		) {
			Icon(
					modifier = Modifier.size(48.dp),
					painter = painterResource(icon),
					contentDescription = text,
					tint = ComposeAppTheme.colors.grey
			)
		}
		Spacer(Modifier.height(32.dp))
		subhead2_grey(
				modifier = Modifier.padding(horizontal = 48.dp),
				text = text,
				textAlign = TextAlign.Center,
				overflow = TextOverflow.Ellipsis,
		)
		actionsComposable?.let { composable ->
			Spacer(Modifier.height(32.dp))
			composable.invoke()
		}
	}
}