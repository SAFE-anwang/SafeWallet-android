package io.horizontalsystems.bankwallet.modules.safe4.node.addlockday

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.title3_leah


@Composable
fun AddLockDayConfirmationDialog(
		day: Int,
		onOKClick: () -> Unit,
		onCancelClick: () -> Unit,
) {

	Dialog(
			onDismissRequest = onCancelClick
	) {
		Column(
				modifier = Modifier
						.clip(RoundedCornerShape(16.dp))
						.background(color = ComposeAppTheme.colors.lawrence)
						.padding(horizontal = 24.dp, vertical = 20.dp)
		) {

			Text(
					text = stringResource(id = R.string.Safe_Four_Node_Add_Lock_Day),
					style = ComposeAppTheme.typography.title3,
					color = ComposeAppTheme.colors.bran,
			)
			Spacer(Modifier.height(16.dp))

			Text(
					text = stringResource(id = R.string.Safe_Four_Node_Add_Lock_Day_Alert, day),
					style = ComposeAppTheme.typography.body,
					color = ComposeAppTheme.colors.bran,
			)


			Spacer(Modifier.height(32.dp))

			Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.End
			) {
				ButtonPrimaryTransparent(
						onClick = onCancelClick,
						title = stringResource(R.string.Safe_Four_Proposal_Cancel)
				)

				Spacer(Modifier.width(8.dp))

				ButtonPrimaryYellow(
						onClick = {
							onOKClick.invoke()
						},
						title = stringResource(R.string.Button_Ok)
				)
			}
		}
	}
}