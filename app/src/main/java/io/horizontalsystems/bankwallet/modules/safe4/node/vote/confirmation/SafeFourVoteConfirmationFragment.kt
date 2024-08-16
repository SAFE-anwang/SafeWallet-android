package io.horizontalsystems.bankwallet.modules.safe4.node.vote.confirmation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.modules.safe4.node.vote.lockedList
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.DisposableLifecycleCallbacks
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay
import org.telegram.ui.Stories.recorder.StoryLinkSheet.WebpagePreviewView.Factory.item

class SafeFourVoteConfirmationFragment: BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<SafeFourVoteConfirmationModule.Input>()
        if (input?.data == null) {
            navController.popBackStack(R.id.createSuperNodeFragment, true)
            return
        }

        val viewModel by viewModels<SafeFourVoteConfirmationViewModel> { SafeFourVoteConfirmationModule.Factory(
                input.title,
                input.isSuper,
                input.wallet,
                input.data,
        ) }
        SafeFourVoteNodeConfirmationScreen(viewModel, navController, input.sendEntryPointDestId)
    }
}

@Composable
fun SafeFourVoteNodeConfirmationScreen(
        viewModel: SafeFourVoteConfirmationViewModel,
        navController: NavController,
        closeUntilDestId: Int
) {
    val uiState = viewModel.uiState
    val voteNum = uiState.voteNum
    val data = viewModel.voteData
    val lockId = uiState.lockIdInfo ?: emptyList()

    val sendResult = viewModel.sendResult
    val view = LocalView.current

    when (sendResult) {
        SendResult.Sending -> {
            HudHelper.showInProcessMessage(
                    view,
                    R.string.Send_Sending,
                    SnackbarDuration.INDEFINITE
            )
        }

        SendResult.Sent -> {
            HudHelper.showSuccessMessage(
                    view,
                    R.string.Send_Success,
                    SnackbarDuration.LONG
            )
        }

        is SendResult.Failed -> {
            HudHelper.showErrorMessage(view, sendResult.caution.getString())
        }

        null -> Unit
    }

    LaunchedEffect(sendResult) {
        if (sendResult == SendResult.Sent) {
            delay(1200)
            navController.popBackStack(closeUntilDestId, true)
        }
    }

    DisposableLifecycleCallbacks(
            //additional close for cases when user closes app immediately after sending
            onResume = {
                if (sendResult == SendResult.Sent) {
                    navController.popBackStack(closeUntilDestId, true)
                }
            }
    )

    val listState = rememberLazyListState()
    Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                        title = uiState.title,
                        navigationIcon = {
                            HsBackButton(onClick = { navController.popBackStack() })
                        }
                )
            },
            bottomBar = {
                Row {
                    ButtonPrimaryYellow(
                            modifier = Modifier
                                    .weight(1f)
                                    .padding(16.dp)
                                    .height(40.dp),
                            title = stringResource(R.string.Safe_Four_Register_Node_Send),
                            onClick = {
                                viewModel.send()
                            }
                    )
                }
            }
    ) {
        LazyColumn(modifier = Modifier.padding(it), state = listState) {
            if (lockId.isNotEmpty()) {
                lockedList(
                        lockIdsList = lockId,
                        onCheckedChange = { id, checked ->

                        },
                        onBottomReached = {

                        }
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                        modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))
                                .background(ComposeAppTheme.colors.lawrence)
                ) {
                    if (lockId.isNullOrEmpty()) {
                        Row(
                                modifier = Modifier
                                        .padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
                            Text(
                                    text = stringResource(id = R.string.Safe_Four_Register_Lock),
                                    style = ComposeAppTheme.typography.body,
                                    color = ComposeAppTheme.colors.grey,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                    text = voteNum,
                                    color = ComposeAppTheme.colors.grey,
                                    style = ComposeAppTheme.typography.body,
                                    maxLines = 1,
                            )
                        }
                        Divider(
                                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                                thickness = 1.dp,
                                color = ComposeAppTheme.colors.steel10,
                        )
                    }
                    body_grey(modifier = Modifier.padding(start = 16.dp),
                            text = stringResource(id = R.string.Safe_Four_Node_Info_Id))

                    body_bran(modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                            text = data.nodeId.toString())
                    Divider(
                            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                            thickness = 1.dp,
                            color = ComposeAppTheme.colors.steel10,
                    )

                    body_grey(modifier = Modifier.padding(start = 16.dp),
                            text = stringResource(id = R.string.Safe_Four_Node_Super_Node_Vote_Address))

                    body_bran(modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                            text = data.dstAddr)
                    Divider(
                            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                            thickness = 1.dp,
                            color = ComposeAppTheme.colors.steel10,
                    )
                    if (viewModel.isSuper) {
                        body_grey(modifier = Modifier.padding(start = 16.dp),
                                text = stringResource(id = R.string.Safe_Four_Register_Mode_Name))

                        body_bran(modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                                text = data.nodeName)
                        Divider(
                                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                                thickness = 1.dp,
                                color = ComposeAppTheme.colors.steel10,
                        )
                    }

                    body_grey(modifier = Modifier.padding(start = 16.dp), text = stringResource(id = R.string.Safe_Four_Register_Introduction))

                    body_bran(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), text = data.nodeDesc)

                }
            }
        }
    }
}