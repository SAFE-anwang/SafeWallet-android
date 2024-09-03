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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeStatus
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.SectionItemPosition
import io.horizontalsystems.bankwallet.ui.compose.components.SectionUniversalItem
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
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
							text = stringResource(R.string.Safe_Four_Proposal_No_Data),
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
			Spacer(modifier = Modifier.height(5.dp))
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
		val clipModifier = Modifier.clip(RoundedCornerShape(12.dp))

		RowUniversal(
				modifier = Modifier
						.fillMaxSize()
						.then(clipModifier)
						.background(ComposeAppTheme.colors.lawrence)
						.clickable(onClick = onClick),
		) {
			Column(
					modifier = Modifier
							.padding(start = 16.dp, end = 16.dp)
							.alpha(1f)
			) {
				Row {
					Text(
							modifier = Modifier
									.weight(3f),
							text = item.title,
							style = ComposeAppTheme.typography.body,
							color = ComposeAppTheme.colors.bran,
							overflow = TextOverflow.Ellipsis,
							maxLines = 1,
							fontWeight = FontWeight.Bold
					)
					Spacer(Modifier.weight(1f))

					val color = when (item.status) {
						is ProposalStatus.Voting -> ComposeAppTheme.colors.issykBlue
						is ProposalStatus.Lose -> ComposeAppTheme.colors.grey50
						is ProposalStatus.Adopt -> ComposeAppTheme.colors.green50
					}
					Row(
							modifier = Modifier
									.clip(RoundedCornerShape(5.dp))
									.wrapContentWidth()
									.background(color)
									.padding(start = 2.dp, top = 1.dp, end = 2.dp, bottom = 2.dp)) {
						Text(
								text = item.status.title().getString(),
								style = ComposeAppTheme.typography.captionSB,
								color = ComposeAppTheme.colors.white,
								overflow = TextOverflow.Ellipsis,
								maxLines = 1,
						)
					}
				}
				Spacer(Modifier.height(2.dp))
				body_grey(
						text = stringResource(id = R.string.Safe_Four_Proposal_List_Amount, item.amount)
				)

				Spacer(Modifier.height(2.dp))

				body_grey(
						text = stringResource(id = R.string.Safe_Four_Proposal_Creator, item.creator.shorten(8))
				)

				Spacer(Modifier.height(2.dp))

				Row {
					body_bran(
							text = stringResource(id = R.string.Safe_Four_Proposal_List_ID, item.id.toString())
					)
					Spacer(Modifier.weight(1f))
					body_grey(
							text = item.endDate,
							overflow = TextOverflow.Ellipsis,
							maxLines = 1,
					)
				}
			}
		}
	}
}
