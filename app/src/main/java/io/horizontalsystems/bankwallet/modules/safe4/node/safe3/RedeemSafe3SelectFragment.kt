package io.horizontalsystems.bankwallet.modules.safe4.node.safe3

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
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
import io.horizontalsystems.bankwallet.modules.safe4.Safe4Module
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_bran
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_grey
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper

class RedeemSafe3SelectFragment(): BaseComposeFragment() {
	@Composable
	override fun GetContent(navController: NavController) {
		RedeemSafe3SelectScreen(navController = navController) {
			Safe4Module.handlerNode(Safe4Module.SafeFourType.Redeem, navController, it)
		}
	}
}

@Composable
fun RedeemSafe3SelectScreen(
		navController: NavController,
		onSelect: (Boolean) -> Unit
) {

	Column(modifier = Modifier
			.background(color = ComposeAppTheme.colors.tyler)) {
		AppBar(
				title = stringResource(id = R.string.Redeem_Safe3_Title),
				navigationIcon = {
					HsBackButton(onClick = { navController.popBackStack() })
				}
		)
		Column(modifier = Modifier
				.padding(16.dp)
				.fillMaxSize()
				.verticalScroll(rememberScrollState())) {

			Spacer(modifier = Modifier.height(16.dp))

			Column(
					modifier = Modifier
							.clip(RoundedCornerShape(8.dp))
							.border(1.dp, ComposeAppTheme.colors.grey50, RoundedCornerShape(8.dp))
							.background(ComposeAppTheme.colors.lawrence)
							.fillMaxWidth()
							.clickable {
								onSelect.invoke(true)
							},
					horizontalAlignment = Alignment.CenterHorizontally
			) {
				Text(
						modifier = Modifier
								.padding(16.dp),
						text = stringResource(id = R.string.Redeem_Safe3_Local_Wallet))
			}

			Spacer(modifier = Modifier.height(16.dp))


			Column(
					modifier = Modifier
							.clip(RoundedCornerShape(8.dp))
							.border(1.dp, ComposeAppTheme.colors.grey50, RoundedCornerShape(8.dp))
							.background(ComposeAppTheme.colors.lawrence)
							.fillMaxWidth()
							.clickable {
								onSelect.invoke(false)
							},
					horizontalAlignment = Alignment.CenterHorizontally
			) {
				Text(
						modifier = Modifier
								.padding(16.dp),
						text = stringResource(id = R.string.Redeem_Safe3_Other_Wallet))
			}
			
		}
	}
}