package io.horizontalsystems.bankwallet.modules.safe4.dapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold

@Composable
fun Safe4DAppManageScreen(
    navController: NavController,
    viewModel: Safe4DAppViewModel = viewModel(factory = Safe4DAppModule.Factory())
) {
    val uiState by viewModel.uiState.collectAsState()

    HSScaffold(
        title = stringResource(R.string.Safe4_DApp_Manage_Title),
        onBack = { navController.popBackStack() },
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Safe4_DApp_Register),
                icon = R.drawable.ic_plus_20,
                tint = ComposeAppTheme.colors.grey,
                onClick = {
                    Safe4DAppRegisterFragment.handler(navController)
                }
            )
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ComposeAppTheme.colors.grey)
                }
            } else if (uiState.error != null && uiState.dApps.isEmpty()) {
                ListErrorView(
                    errorText = uiState.error ?: stringResource(R.string.SyncError),
                    onClick = { viewModel.refresh() }
                )
            } else if (uiState.dApps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.Safe4_DApp_Empty_List),
                        style = ComposeAppTheme.typography.body,
                        color = ComposeAppTheme.colors.grey
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp)
                ) {
                    items(uiState.dApps, key = { it.id }) { dapp ->
                        ManagedDAppCell(
                            dapp = dapp,
                            onClick = {
                                /*val bundle = android.os.Bundle()
                                bundle.putString("url", dapp.url)
                                bundle.putString("name", dapp.name)
                                navController.navigate(R.id.dappBrowseFragment, bundle)*/
                            },
                            onEdit = {
                                Safe4DAppRegisterFragment.handler(navController, dapp)
                            },
                            onRemove = {
                                viewModel.requestDeleteDApp(dapp)
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    uiState.showDeleteConfirmation?.let { dapp ->
        Dialog(onDismissRequest = { viewModel.dismissDeleteDialog() }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(color = ComposeAppTheme.colors.lawrence)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.Safe4_DApp_Delete_Confirm_Title),
                    style = ComposeAppTheme.typography.body,
                    color = ComposeAppTheme.colors.leah
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.Safe4_DApp_Delete_Confirm_Message, dapp.name),
                    style = ComposeAppTheme.typography.subheadB,
                    color = ComposeAppTheme.colors.grey
                )

                Spacer(Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    ButtonPrimaryTransparent(
                        onClick = { viewModel.dismissDeleteDialog() },
                        title = stringResource(R.string.Safe_Four_Proposal_Cancel)
                    )

                    Spacer(Modifier.width(8.dp))

                    ButtonPrimaryYellow(
                        onClick = { viewModel.confirmDeleteDApp() },
                        title = stringResource(R.string.Button_Delete)
                    )
                }
            }
        }
    }
}

@Composable
private fun ManagedDAppCell(
    dapp: ManagedDAppItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = ComposeAppTheme.colors.lawrence
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                if (dapp.iconUrl.isNotEmpty()) {
                    HsImage(
                        url = dapp.iconUrl,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ComposeAppTheme.colors.elenaD),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dapp.name.take(1).uppercase(),
                            style = ComposeAppTheme.typography.headline2,
                            color = ComposeAppTheme.colors.grey
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dapp.name,
                        style = ComposeAppTheme.typography.body,
                        color = ComposeAppTheme.colors.leah,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = dapp.url,
                        style = ComposeAppTheme.typography.caption,
                        color = ComposeAppTheme.colors.grey,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (dapp.description.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = dapp.description,
                            style = ComposeAppTheme.typography.micro,
                            color = ComposeAppTheme.colors.grey,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Actions
                IconButton(onClick = onEdit) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit_20),
                        contentDescription = "Edit",
                        tint = ComposeAppTheme.colors.grey
                    )
                }

                IconButton(onClick = {
                    onRemove()
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete_20),
                        contentDescription = "Remove",
                        tint = ComposeAppTheme.colors.lucian
                    )
                }
            }

            // Status badge at top-right corner
            StatusBadge(
                status = dapp.status,
                fraudNum = dapp.fraudNum,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
            )
        }
    }
}

@Composable
private fun StatusBadge(
    status: String,
    fraudNum: Long = 0,
    modifier: Modifier = Modifier
) {
    if (status != "frozen" && fraudNum <= 0) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (status == "frozen") {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFF3B30).copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Frozen",
                    style = ComposeAppTheme.typography.microSB,
                    color = Color(0xFFFF3B30)
                )
            }
        }
        if (fraudNum > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFF6D00).copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Fraud ×$fraudNum",
                    style = ComposeAppTheme.typography.microSB,
                    color = Color(0xFFFF6D00)
                )
            }
        }
    }
}


