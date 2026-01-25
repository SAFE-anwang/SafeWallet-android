package io.horizontalsystems.bankwallet.modules.safe4.node

import android.graphics.fonts.FontStyle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.SafeFourVoteFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow2
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.SectionItemPosition
import io.horizontalsystems.bankwallet.ui.compose.components.SectionUniversalItem
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.bankwallet.ui.compose.components.Tabs
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey50
import io.horizontalsystems.bankwallet.ui.compose.components.body_issykBlue
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
	val isRegisterNode = uiState.isRegisterNode

	val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
	val coroutineScope = rememberCoroutineScope()

	val focusRequester = remember { FocusRequester() }

	Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
		AppBar(
				title = title,
				navigationIcon = {
					HsBackButton(onClick = { navController.navigateUp() })
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

		HintView(viewModel.getRegisterHintText())

		if (isRegisterNode.first || isRegisterNode.second) {
			HintView(viewModel.getAlreadyRegisterText())
		}
		val selectedTab = tabs[pagerState.currentPage]
		val tabItems = tabs.map {
			TabItem(stringResource(id = it.second), it == selectedTab, it)
		}
		Tabs(tabItems, onClick = { tab ->
			coroutineScope.launch {
				pagerState.scrollToPage(tab.first)
			}
		})
		Spacer(modifier = Modifier.height(2.dp))
		SearchBar(
				searchHintText = stringResource(if(viewModel.isSuperNode()) R.string.Super_Node_Search else R.string.Master_Node_Search),
				focusRequester = focusRequester,
				onClose = { viewModel.clearQuery() },
				onSearchTextChanged = { query -> viewModel.searchByQuery(query) }
		)
		Spacer(modifier = Modifier.height(2.dp))
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
								text = stringResource(
										if (viewModel.isSuperNode())
											R.string.Safe_Four_No_Super_Node
										else
											R.string.Safe_Four_No_Master_Node),
								icon = R.drawable.ic_no_data
						)
					}
				}
			} else {
				val listState = rememberLazyListState()
				LazyColumn(Modifier.padding(paddingValues), state = listState) {

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
												navController.context.getString(viewModel.getJoinButtonName()),
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
												it.address.hex,
											it.id,
											it.incentivePlan
										)
								)
							},
							onAddLockDayClick = {
								navController.slideFromBottom(
										R.id.addLockDayFragment,
										SafeFourModule.AddLockDayInput(
												it.founders.filter { it.addr.hex == viewModel.receiveAddress() }.map { it.lockID },
												viewModel.wallet
										)
								)
							},
							isRegisterSuperNode = isRegisterNode.first,
							isMine = isMine
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
		onAddLockDayClick: (NodeViewItem) -> Unit,
		isSuperNode: Boolean,
		isRegisterSuperNode: Boolean,
		isMine: Boolean
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
					NodeCell(item, position, isRegisterSuperNode, isMine,
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
							},
							onAddLockDayClick = {
								onAddLockDayClick.invoke(item)
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
						},
						onAddLockDayClick = {
							onAddLockDayClick.invoke(item)
						}
					)
				}
			}

			Spacer(modifier = Modifier.height(5.dp))

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
fun NodeCell(item: NodeViewItem, position: SectionItemPosition,
			 isRegisterSuperNode: Boolean,
			 isMine: Boolean,
			 onClick: () -> Unit,
			 voteClick: () -> Unit,
			 joinClick: () -> Unit,
			 onEditClick: () -> Unit,
			 onAddLockDayClick: () -> Unit,
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
				if (!isMine) {
					body_bran(text = stringResource(id = R.string.Safe_Four_Node_Info_Ranking, item.ranking))
					Spacer(Modifier.height(2.dp))
				}
				Row {
					body_bran(text = stringResource(id = R.string.Safe_Four_Node_Info_Id_List, item.id.toString()))
					Spacer(modifier = Modifier.weight(1f))
					val color = when (item.status) {
						is NodeStatus.Online -> ComposeAppTheme.colors.issykBlue
						is NodeStatus.Exception -> ComposeAppTheme.colors.grey50
					}
					Row(
							modifier = Modifier
									.clip(RoundedCornerShape(5.dp))
									.background(color)
									.padding(start = 2.dp, top = 1.dp, end = 2.dp, bottom = 2.dp),
							horizontalArrangement = Arrangement.Center) {
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
				body_bran(text = stringResource(id = R.string.Safe_Four_Node_Info_Name_List, item.name),
						maxLines = 1,
						overflow = TextOverflow.Ellipsis)
				Spacer(Modifier.height(2.dp))
				Row {
					if (item.isMine) {
						body_issykBlue(text = stringResource(id = R.string.Safe_Four_Node_Info_Address_List, item.address.hex.shorten(8)))
						Spacer(Modifier.weight(1f))
						if (item.isPartner) {
							Row(
									modifier = Modifier
											.clip(RoundedCornerShape(5.dp))
											.background(ComposeAppTheme.colors.issykBlue)
											.padding(start = 2.dp, top = 1.dp, end = 2.dp, bottom = 2.dp),
									horizontalArrangement = Arrangement.Center) {
								Text(
										text = stringResource(id = R.string.Safe_Four_Node_Partner),
										style = ComposeAppTheme.typography.captionSB,
										color = ComposeAppTheme.colors.white,
										overflow = TextOverflow.Ellipsis,
										maxLines = 1,
								)
							}
						}
						if (item.isCreator) {
							Row(
									modifier = Modifier
											.clip(RoundedCornerShape(5.dp))
											.background(ComposeAppTheme.colors.issykBlue)
											.padding(start = 2.dp, top = 1.dp, end = 2.dp, bottom = 2.dp),
									horizontalArrangement = Arrangement.Center) {
								Text(
										text = stringResource(id = R.string.Safe_Four_Node_Creator),
										style = ComposeAppTheme.typography.captionSB,
										color = ComposeAppTheme.colors.white,
										overflow = TextOverflow.Ellipsis,
										maxLines = 1,
								)
							}
						}
					} else {
						body_bran(text = stringResource(id = R.string.Safe_Four_Node_Info_Address_List, item.address.hex.shorten(8)))
					}
				}
				Spacer(Modifier.height(2.dp))
				Row {
					body_bran(
							text = stringResource(id = R.string.Safe_Four_Node_Info_Vote_List, item.voteCount),
							maxLines = 1)
					Spacer(Modifier.weight(1f))
					body_bran(
							text = stringResource(id = R.string.Safe_Four_Node_Info_Pledge_List, item.voteCompleteCount),
							maxLines = 1)
					/*body_bran(
							modifier = Modifier
									.weight(1f),
							text = item.progressText,
							textAlign = TextAlign.End,
							maxLines = 1)*/
				}
				Spacer(Modifier.height(2.dp))
				Row{
					Column(
							horizontalAlignment = Alignment.CenterHorizontally
					) {
						Row(
								verticalAlignment = Alignment.CenterVertically) {

							LinearProgressIndicator(
									modifier = Modifier
											.weight(1f)
											.clip(RoundedCornerShape(8.dp))
											.border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))
											.background(ComposeAppTheme.colors.lawrence),
									progress = item.progress,
									color = ComposeAppTheme.colors.issykBlue,
									backgroundColor = ComposeAppTheme.colors.grey50)
							Spacer(Modifier.width(8.dp))
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
				Spacer(Modifier.height(2.dp))

				Row {
						ButtonPrimaryYellow2(
								modifier = Modifier
										.weight(1.7f)
										.height(25.dp),
								title = stringResource(R.string.Safe_Four_Node_Join_Partner),
								onClick = {
									joinClick.invoke()
								},
								enabled = item.canJoin
						)
						Spacer(Modifier.width(5.dp))
						ButtonPrimaryYellow2(
								modifier = Modifier
										.weight(1f)
										.height(25.dp),
								title = stringResource(R.string.Safe_Four_Vote),
								onClick = {
									voteClick.invoke()
								},
								enabled = item.isVoteEnable
						)
						Spacer(Modifier.width(5.dp))
						ButtonPrimaryYellow2(
								modifier = Modifier
										.weight(1f)
										.height(25.dp),
								title = stringResource(R.string.Safe_Four_Node_Edit),
								onClick = {
									onEditClick.invoke()
								},
								enabled = item.isEdit
						)
						Spacer(Modifier.width(5.dp))
						ButtonPrimaryYellow2(
								modifier = Modifier
										.weight(1.5f)
										.height(25.dp),
								title = stringResource(R.string.Safe_Four_Node_Add_Lock_Day),
								onClick = {
									onAddLockDayClick.invoke()
								},
								enabled = item.isAddLockDay
						)
					}
			}
		}

		Spacer(Modifier.height(5.dp))
	}
}


@Composable
fun MasterNodeCell(item: NodeViewItem, position: SectionItemPosition,
				   onClick: () -> Unit,
				   joinClick: () -> Unit,
				   onEditClick: () -> Unit,
				   onAddLockDayClick: () -> Unit,
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
					body_bran(text = stringResource(id = R.string.Safe_Four_Node_Info_Id_List, item.id.toString()))
					Spacer(Modifier.weight(1f))
					val color = when (item.status) {
						is NodeStatus.Online -> ComposeAppTheme.colors.issykBlue
						is NodeStatus.Exception -> ComposeAppTheme.colors.grey50
					}
					Row(
							modifier = Modifier
									.clip(RoundedCornerShape(5.dp))
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

				Row {
					if (item.isMine) {
						body_issykBlue(text = stringResource(id = R.string.Safe_Four_Node_Info_Address_List, item.address.hex.shorten(8)))
						Spacer(Modifier.weight(1f))
						if (item.isPartner) {
							Row(
									modifier = Modifier
											.clip(RoundedCornerShape(5.dp))
											.background(ComposeAppTheme.colors.issykBlue)
											.padding(start = 2.dp, top = 1.dp, end = 2.dp, bottom = 2.dp),
									horizontalArrangement = Arrangement.Center) {
								Text(
										text = stringResource(id = R.string.Safe_Four_Node_Partner),
										style = ComposeAppTheme.typography.captionSB,
										color = ComposeAppTheme.colors.white,
										overflow = TextOverflow.Ellipsis,
										maxLines = 1,
								)
							}
						}
						if (item.isCreator) {
							Row(
									modifier = Modifier
											.clip(RoundedCornerShape(5.dp))
											.background(ComposeAppTheme.colors.issykBlue)
											.padding(start = 2.dp, top = 1.dp, end = 2.dp, bottom = 2.dp),
									horizontalArrangement = Arrangement.Center) {
								Text(
										text = stringResource(id = R.string.Safe_Four_Node_Creator),
										style = ComposeAppTheme.typography.captionSB,
										color = ComposeAppTheme.colors.white,
										overflow = TextOverflow.Ellipsis,
										maxLines = 1,
								)
							}
						}
					} else {
						body_bran(text = stringResource(id = R.string.Safe_Four_Node_Info_Address_List, item.address.hex.shorten(8)))
					}
				}
				Spacer(Modifier.height(2.dp))
				Row {
					body_bran(
							text = stringResource(id = R.string.Safe_Four_Node_Info_Vote_List, item.voteCount))
					Spacer(Modifier.weight(1f))
					body_bran(
							text = stringResource(id = R.string.Safe_Four_Node_Info_Pledge_List, item.voteCompleteCount))
				}

				Spacer(Modifier.height(2.dp))
				Row {
					ButtonPrimaryYellow2(
							modifier = Modifier
									.weight(1f)
									.height(25.dp),
							title = stringResource(R.string.Safe_Four_Node_Join_Partner),
							onClick = {
								joinClick.invoke()
							},
							enabled = item.canJoin
					)
					Spacer(Modifier.width(10.dp))
					ButtonPrimaryYellow2(
							modifier = Modifier
									.weight(1f)
									.height(25.dp),
							title = stringResource(R.string.Safe_Four_Node_Edit),
							onClick = {
								onEditClick.invoke()
							},
							enabled = item.isEdit
					)
					Spacer(Modifier.width(10.dp))
					ButtonPrimaryYellow2(
							modifier = Modifier
									.weight(1f)
									.height(25.dp),
							title = stringResource(R.string.Safe_Four_Node_Add_Lock_Day),
							onClick = {
								onAddLockDayClick.invoke()
							},
							enabled = item.isAddLockDay
					)
				}

			}
		}

		Spacer(Modifier.height(5.dp))
	}
}