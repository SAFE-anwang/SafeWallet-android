package io.horizontalsystems.bankwallet.modules.safe4.node.proposal

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.SectionItemPosition
import io.horizontalsystems.bankwallet.ui.compose.components.SectionUniversalItem
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.sectionItemBorder

@Composable
fun SafeFourProposalScreen(
		viewModel: SafeFourProposalViewModel,
		navController: NavController,
		isMine: Boolean = false
) {
	val uiState = viewModel.uiState
	val nodeList = if (isMine) {
		uiState.mineProposalList
	} else {
		uiState.allProposalList
	}

	Scaffold(
			backgroundColor = ComposeAppTheme.colors.tyler,
	) { paddingValues ->
		if (nodeList.isNullOrEmpty()) {
			Column(Modifier.padding(paddingValues)) {
				if (nodeList == null) {
					ListEmptyView(
							text = stringResource(R.string.Transactions_WaitForSync),
							icon = R.drawable.ic_clock
					)
				} else {
					ListEmptyView(
							text = stringResource(R.string.Safe_Four_No_Super_Node),
							icon = R.drawable.ic_no_data
					)
				}
			}
		} else {
			val listState = rememberLazyListState()
			LazyColumn(Modifier.padding(paddingValues), state = listState) {
				proposalList(
						nodeList = nodeList,
						onClick = {
							navController.slideFromBottom(
									R.id.proposalInfoFragment,
									SafeFourProposalModule.InfoInput(viewModel.wallet, viewModel.getProposalInfo(it.id, if (isMine) 1 else 0))
							)
						},
						onBottomReached = { viewModel.onBottomReached() },
				)
			}
		}
	}
}


@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.proposalList(
		nodeList: List<ProposalViewItem>,
		onClick: (ProposalViewItem) -> Unit,
		onBottomReached: () -> Unit,
) {
	val bottomReachedRank = getBottomReachedRank(nodeList)

	val itemsCount = nodeList.size
		val singleElement = itemsCount == 1
		itemsIndexed(
				items = nodeList,
				key = { _, item ->
					item.id
				}
		) { index, item ->
			val position: SectionItemPosition = when {
				singleElement -> SectionItemPosition.Single
				index == 0 -> SectionItemPosition.First
				index == itemsCount - 1 -> SectionItemPosition.Last
				else -> SectionItemPosition.Middle
			}

			Box(modifier = Modifier.padding(horizontal = 16.dp)) {
				ProposalItemCell(item, position,
						onClick = {
							onClick.invoke(item)
						}
				)
			}
			Divider(
					modifier = Modifier.padding(start = 16.dp, end = 16.dp),
					thickness = 1.dp,
					color = ComposeAppTheme.colors.steel10,
			)

			if (item.id == bottomReachedRank) {
				onBottomReached.invoke()
			}
		}

		item {
			Spacer(modifier = Modifier.height(12.dp))
		}
}

private fun getBottomReachedRank(nodeList: List<ProposalViewItem>): Int? {
//	val txList = nodeList.values.flatten()
	//get index not exact bottom but near to the bottom, to make scroll smoother
	val index = if (nodeList.size > 4) nodeList.size - 4 else 0

	return nodeList.getOrNull(index)?.id
}


@Composable
fun ProposalItemCell(
		item: ProposalViewItem,
		position: SectionItemPosition,
		onClick: () -> Unit
) {
	val divider = position == SectionItemPosition.Middle || position == SectionItemPosition.Last
	SectionUniversalItem(
			borderTop = divider,
	) {
		val clipModifier = when (position) {
			SectionItemPosition.First -> {
				Modifier.clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
			}

			SectionItemPosition.Last -> {
				Modifier.clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
			}

			SectionItemPosition.Single -> {
				Modifier.clip(RoundedCornerShape(12.dp))
			}

			else -> Modifier
		}

		val borderModifier = if (position != SectionItemPosition.Single) {
			Modifier.sectionItemBorder(1.dp, ComposeAppTheme.colors.steel20, 12.dp, position)
		} else {
			Modifier.border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(12.dp))
		}

		RowUniversal(
				modifier = Modifier
						.fillMaxSize()
						.then(clipModifier)
						.then(borderModifier)
						.background(ComposeAppTheme.colors.lawrence)
						.clickable(onClick = onClick),
		) {
			Column(
					modifier = Modifier
							.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp)
							.alpha(1f)
			) {
				Row {
					body_leah(
							modifier = Modifier.fillMaxWidth().weight(1f),
							text = item.id.toString(),
							maxLines = 1,
					)
					val color = ComposeAppTheme.colors.grey
					Text(
							modifier = Modifier.fillMaxWidth().weight(2f),
							text = item.title,
							style = ComposeAppTheme.typography.body,
							color = color,
							overflow = TextOverflow.Ellipsis,
							maxLines = 1,
					)
					Text(
							modifier = Modifier.fillMaxWidth().weight(1.5f),
							text = item.amount,
							style = ComposeAppTheme.typography.body,
							color = color,
							overflow = TextOverflow.Ellipsis,
							maxLines = 1,
					)
				}

				Spacer(Modifier.height(6.dp))
				Row {
					val color = when (item.status) {
						is ProposalStatus.Voting -> ComposeAppTheme.colors.tgBlue
						is ProposalStatus.Lose -> ComposeAppTheme.colors.grey50
						is ProposalStatus.Adopt -> ComposeAppTheme.colors.greenD
					}
					Text(
							modifier = Modifier.fillMaxWidth().weight(1f),
							text = item.status.title().getString(),
							style = ComposeAppTheme.typography.body,
							color = color,
							overflow = TextOverflow.Ellipsis,
							maxLines = 1,
					)
					Text(
							modifier = Modifier.fillMaxWidth().weight(2f),
							text = item.creator.shorten(),
							style = ComposeAppTheme.typography.body,
							color = ComposeAppTheme.colors.grey,
							overflow = TextOverflow.Ellipsis,
							maxLines = 1,
					)

					Text(
							modifier = Modifier.fillMaxWidth().weight(1.5f),
							text = item.endDate,
							style = ComposeAppTheme.typography.body,
							color = ComposeAppTheme.colors.grey,
							overflow = TextOverflow.Ellipsis,
							maxLines = 1,
					)
				}

			}
		}
	}
}
