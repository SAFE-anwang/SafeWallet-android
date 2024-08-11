package io.horizontalsystems.bankwallet.modules.safe4.node.proposal.info

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
fun ProposalConfirmationDialog(
		viewModel: SafeFourProposalInfoViewModel,
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
			val (buttonText, color) = when(viewModel.getVoteType()) {
				0 -> Pair(R.string.Safe_Four_Proposal_Vote_Agree, ComposeAppTheme.colors.greenD)
				1 -> Pair(R.string.Safe_Four_Proposal_Vote_Refuse, ComposeAppTheme.colors.redD)
				else -> Pair(R.string.Safe_Four_Proposal_Vote_Give_up, ComposeAppTheme.colors.grey)
			}
			Text(
					text = stringResource(id = buttonText),
					style = ComposeAppTheme.typography.title3,
					color = color,
			)
			Spacer(Modifier.height(12.dp))

			ContentScreen(viewModel = viewModel, true)


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
						title = stringResource(R.string.Safe_Four_Proposal_Send)
				)
			}
		}
	}
}