package io.horizontalsystems.bankwallet.modules.safe4.node

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey50

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
		OutlinedTextField(
				modifier = Modifier
						.fillMaxWidth()
						.focusRequester(focusRequester),
				value = searchText,
				onValueChange = {
					searchText = it
					onSearchTextChanged.invoke(it)
//					showClearButton = it.isNotEmpty()
				},
				placeholder = {
					body_grey50(
							text = searchHintText,
							maxLines = 1,
							overflow = TextOverflow.Ellipsis
					)
				},
				textStyle = ComposeAppTheme.typography.body,
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
									painter = painterResource(icon),
									contentDescription = stringResource(R.string.Button_Cancel),
									tint = ComposeAppTheme.colors.jacob
							)
						}

					}
				},
		)

		LaunchedEffect(Unit) {
			focusRequester.requestFocus()
		}
	}
}