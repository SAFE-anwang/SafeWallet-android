package io.horizontalsystems.bankwallet.modules.safe4.node

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.CreatorScreen
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.LockVoteScreen
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.MasterVoteScreen
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.SafeFourVoteFragment
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.SafeFourVoteModule
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.SafeFourVoteRecordViewModel
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.SafeFourVoteViewModel
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.VoteScreen
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.VoterRecordScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.SectionItemPosition
import io.horizontalsystems.bankwallet.ui.compose.components.SectionUniversalItem
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.bankwallet.ui.compose.components.Tabs
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.sectionItemBorder
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabScreen(
		viewModel: SafeFourNodeViewModel,
		navController: NavController
) {

	val tabs = viewModel.tabs

	val uiState = viewModel.uiState
	val title = uiState.title

	val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
	val coroutineScope = rememberCoroutineScope()

	Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
		AppBar(
				title = title,
				navigationIcon = {
					HsBackButton(onClick = { navController.popBackStack() })
				},
				menuItems = listOf(
						MenuItem(
								title = TranslatableString.ResString(viewModel.getMenuName()),
								onClick = {
									if (viewModel.menuEnable()) {
										navController.slideFromBottom(
												R.id.createSuperNodeFragment,
												SafeFourModule.CreateInput(viewModel.wallet, viewModel.isSuperNode()))
									}
								},
								enabled = viewModel.menuEnable()
						)
				)
		)
		val selectedTab = tabs[pagerState.currentPage]
		val tabItems = tabs.map {
			TabItem(stringResource(id = it.second), it == selectedTab, it)
		}
		Tabs(tabItems, onClick = { tab ->
			coroutineScope.launch {
				pagerState.scrollToPage(tab.first)
			}
		})

		HorizontalPager(
				state = pagerState,
				userScrollEnabled = false
		) { page ->
			when (tabs[page].first) {
				0 -> {
					SafeFourNodeScreen(viewModel, navController)
				}

				1 -> {
					SafeFourNodeScreen(viewModel, navController, true)
				}
			}
		}
	}
}

@Composable
fun SafeFourNodeScreen(
		viewModel: SafeFourNodeViewModel,
		navController: NavController,
		isMine: Boolean = false
) {
	val uiState = viewModel.uiState
	val nodeList = if (isMine) uiState.mineList else uiState.nodeList
	val isRegisterNode = uiState.isRegisterNode
	Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
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
					item {
						if (isRegisterNode.first || isRegisterNode.second) {
							Column(
									modifier = Modifier
											.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
											.clip(RoundedCornerShape(8.dp))
											.border(1.dp, ComposeAppTheme.colors.yellow50, RoundedCornerShape(8.dp))
											.background(ComposeAppTheme.colors.yellow20)
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
													.padding(16.dp),
											text = stringResource(id = viewModel.getAlreadyRegisterText()))
								}
							}
						}
					}
					nodeList(
							nodeList = nodeList,
							onClick = { onNodeClick(it, viewModel, navController) },
							onBottomReached = { viewModel.onBottomReached() },
							voteClick = {
								navController.slideFromBottom(
										R.id.voteFragment,
										SafeFourVoteFragment.Input(
												viewModel.wallet,
												navController.context.getString(viewModel.getVoteButtonName()),
												it.id,
												viewModel.getNodeType(),
												it.address.hex
										)
								)
							},
							joinClick = {
								navController.slideFromBottom(
										R.id.voteFragment,
										SafeFourVoteFragment.Input(
												viewModel.wallet,
												navController.context.getString(viewModel.getVoteButtonName()),
												it.id,
												viewModel.getNodeType(),
												it.address.hex,
												true
										)
								)
							},
							isSuperNode = viewModel.isSuperNode(),
							onEditClick = {
								navController.slideFromBottom(
										R.id.nodeEditFragment,
										SafeFourNodeEditFragment.Input(
												viewModel.wallet,
												it.name,
												it.desc,
												it.enode,
												viewModel.getNodeType(),
												it.address.hex
										)
								)
							}
					)
				}
			}
		}
	}
}

private fun onNodeClick(
		nodeViewItem: NodeViewItem,
		nodeViewModel: SafeFourNodeViewModel,
		navController: NavController
) {
	navController.slideFromBottom(R.id.nodeInfoFragment,
			SafeFourNodeInfoFragment.Input(nodeViewModel.wallet, nodeViewItem.id, nodeViewModel.getNodeType(), nodeViewItem.address.hex)
			)
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.nodeList(
		nodeList: List<NodeViewItem>,
		onClick: (NodeViewItem) -> Unit,
		onBottomReached: () -> Unit,
		voteClick: (NodeViewItem) -> Unit,
		joinClick: (NodeViewItem) -> Unit,
		onEditClick: (NodeViewItem) -> Unit,
		isSuperNode: Boolean
) {
	val bottomReachedRank = getBottomReachedRank(nodeList)

	val itemsCount = nodeList.size
		val singleElement = itemsCount == 1
		itemsIndexed(
				items = nodeList,
				key = { _, item ->
					item.ranking
				}
		) { index, item ->
			val position: SectionItemPosition = when {
				singleElement -> SectionItemPosition.Single
				index == 0 -> SectionItemPosition.First
				index == itemsCount - 1 -> SectionItemPosition.Last
				else -> SectionItemPosition.Middle
			}

			Box(modifier = Modifier.padding(horizontal = 16.dp)) {
				if (isSuperNode) {
					NodeCell(item, position,
							onClick = {
								onClick.invoke(item)
							},
							voteClick = {
								voteClick.invoke(item)
							},
							joinClick = {
								joinClick.invoke(item)
							},
							onEditClick = {
								onEditClick.invoke(item)
							}
					)
				} else {
					MasterNodeCell(item, position,
						onClick = {
							onClick.invoke(item)
						},
						joinClick = {
							joinClick.invoke(item)
						},
						onEditClick = {
							onEditClick.invoke(item)
						}
					)
				}
			}

			Divider(
					modifier = Modifier.padding(start = 16.dp, end = 16.dp),
					thickness = 1.dp,
					color = ComposeAppTheme.colors.steel10,
			)

			if (item.ranking == bottomReachedRank) {
				onBottomReached.invoke()
			}
		}

		item {
			Spacer(modifier = Modifier.height(12.dp))
		}
}

private fun getBottomReachedRank(nodeList: List<NodeViewItem>): Int? {
//	val txList = nodeList.values.flatten()
	//get index not exact bottom but near to the bottom, to make scroll smoother
	val index = if (nodeList.size > 4) nodeList.size - 4 else 0

	return nodeList.getOrNull(index)?.ranking
}


@Composable
fun NodeCell(item: NodeViewItem, position: SectionItemPosition, onClick: () -> Unit,
			 voteClick: () -> Unit,
			 joinClick: () -> Unit,
			 onEditClick: () -> Unit,
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
							.padding(start = 16.dp, end = 16.dp)
							.alpha(1f)
			) {
				Row{
					Column(
							modifier = Modifier.weight(1f)
					) {
						body_leah(
								modifier = Modifier.padding(end = 16.dp),
								text = item.ranking.toString(),
								maxLines = 1,
						)

						val color = when (item.status) {
							is NodeStatus.Online -> ComposeAppTheme.colors.greenD
							is NodeStatus.Exception -> ComposeAppTheme.colors.redD
						}
						Text(
								text = item.status.title().getString(),
								style = ComposeAppTheme.typography.body,
								color = color,
								overflow = TextOverflow.Ellipsis,
								maxLines = 1,
						)
					}
					Column(
							modifier = Modifier.weight(4f),
							horizontalAlignment = Alignment.CenterHorizontally
					) {
						Row(
								verticalAlignment = Alignment.CenterVertically) {

							Text(
									text = item.voteCount,
									style = ComposeAppTheme.typography.body,
									color = ComposeAppTheme.colors.grey,
									overflow = TextOverflow.Ellipsis,
									maxLines = 1,
							)
							Spacer(Modifier.weight(1f))
							Text(
									text = item.voteCompleteCount,
									style = ComposeAppTheme.typography.body,
									color = ComposeAppTheme.colors.bran,
									overflow = TextOverflow.Ellipsis,
									maxLines = 1,
							)
						}
						Row(
								verticalAlignment = Alignment.CenterVertically) {


							LinearProgressIndicator(
									modifier = Modifier
											.weight(1f)
											.clip(RoundedCornerShape(8.dp))
											.border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))
											.background(ComposeAppTheme.colors.lawrence),
									progress = item.progress,
									color = ComposeAppTheme.colors.green50,
									backgroundColor = ComposeAppTheme.colors.grey50)

							Text(
									modifier = Modifier
											.wrapContentWidth(),
									text = item.progressText,
									style = ComposeAppTheme.typography.body,
									color = ComposeAppTheme.colors.grey,
									overflow = TextOverflow.Ellipsis,
									maxLines = 1,
							)
						}
					}
				}

				Row {
					Column(
							modifier = Modifier.weight(1f)
					) {
						Text(
								text = item.name,
								style = ComposeAppTheme.typography.body,
								color = ComposeAppTheme.colors.grey,
								overflow = TextOverflow.Ellipsis,
								maxLines = 1,
						)
						Spacer(Modifier.height(5.dp))
						Text(
								text = item.desc,
								style = ComposeAppTheme.typography.body,
								color = ComposeAppTheme.colors.grey,
								overflow = TextOverflow.Ellipsis,
								maxLines = 1,
						)
					}
					Column(
							modifier = Modifier.weight(1f)
					) {
						Row {
							Text(
									text = item.address.hex.shorten(),
									style = ComposeAppTheme.typography.body,
									color = ComposeAppTheme.colors.grey,
									overflow = TextOverflow.Ellipsis,
									maxLines = 1,
							)
							Spacer(Modifier.weight(1f))
							body_leah(
									modifier = Modifier.padding(end = 4.dp),
									text = "ID:",
									maxLines = 1,
							)
							body_leah(
									modifier = Modifier.padding(end = 8.dp),
									text = "${item.id}",
									maxLines = 1,
							)
						}

						Row {
							Spacer(Modifier.weight(1f))
							if (item.canJoin) {

								ButtonPrimaryYellow(
										modifier = Modifier
												.wrapContentWidth()
												.height(30.dp),
										title = stringResource(R.string.Safe_Four_Node_Join_Partner),
										onClick = {
											joinClick.invoke()
										}
								)
							} else {
								ButtonPrimaryYellow(
										modifier = Modifier
												.height(30.dp),
										title = stringResource(R.string.Safe_Four_Vote),
										onClick = {
											voteClick.invoke()
										}
								)
							}
							if (item.isEdit) {

								Spacer(Modifier.width(10.dp))
								ButtonPrimaryYellow(
										modifier = Modifier
												.wrapContentWidth()
												.height(30.dp),
										title = stringResource(R.string.Safe_Four_Node_Edit),
										onClick = {
											onEditClick.invoke()
										}
								)
							}
						}
					}
				}
			}
		}
	}
}


@Composable
fun MasterNodeCell(item: NodeViewItem, position: SectionItemPosition,
				   onClick: () -> Unit,
				   joinClick: () -> Unit,
				   onEditClick: () -> Unit,
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
							.padding(start = 16.dp, end = 16.dp)
							.alpha(1f)
			) {
				Row {
					val color = when (item.status) {
						is NodeStatus.Online -> ComposeAppTheme.colors.greenD
						is NodeStatus.Exception -> ComposeAppTheme.colors.grey
					}
					Text(
							text = item.status.title().getString(),
							style = ComposeAppTheme.typography.body,
							color = color,
							overflow = TextOverflow.Ellipsis,
							maxLines = 1,
					)
					Spacer(Modifier.weight(1f))
					body_leah(
							modifier = Modifier.padding(end = 4.dp),
							text = "ID:",
							maxLines = 1,
					)
					body_leah(
							modifier = Modifier.padding(end = 8.dp),
							text = "${item.id}",
							maxLines = 1,
					)
					Spacer(Modifier.weight(1f))
					
					Text(
							text = item.address.hex.shorten(),
							style = ComposeAppTheme.typography.body,
							color = ComposeAppTheme.colors.grey,
							overflow = TextOverflow.Ellipsis,
							maxLines = 1,
					)
					
				}
				Row {
					Text(
							text = stringResource(id = R.string.Safe_Four_Node_Pledge),
							style = ComposeAppTheme.typography.body,
							color = ComposeAppTheme.colors.grey,
							overflow = TextOverflow.Ellipsis,
							maxLines = 1,
					)
					Text(
							text = item.voteCount,
							style = ComposeAppTheme.typography.body,
							color = ComposeAppTheme.colors.grey,
							overflow = TextOverflow.Ellipsis,
							maxLines = 1,
					)
					if (item.canJoin) {
						Spacer(Modifier.weight(1f))

						ButtonPrimaryYellow(
								modifier = Modifier
										.wrapContentWidth()
										.height(30.dp),
								title = stringResource(R.string.Safe_Four_Node_Join_Partner),
								onClick = {
									joinClick.invoke()
								}
						)
					}
					if (item.isEdit) {
						Spacer(Modifier.weight(1f))
						ButtonPrimaryYellow(
								modifier = Modifier
										.wrapContentWidth()
										.height(30.dp),
								title = stringResource(R.string.Safe_Four_Node_Edit),
								onClick = {
									onEditClick.invoke()
								}
						)
					}
				}
			}
		}
	}
}