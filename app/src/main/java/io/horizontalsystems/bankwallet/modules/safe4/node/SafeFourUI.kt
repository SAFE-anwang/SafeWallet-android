package io.horizontalsystems.bankwallet.modules.safe4.node

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

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