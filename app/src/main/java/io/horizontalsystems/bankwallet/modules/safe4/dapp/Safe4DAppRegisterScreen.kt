package io.horizontalsystems.bankwallet.modules.safe4.dapp

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper

@Composable
fun Safe4DAppRegisterScreen(
    navController: NavController,
    viewModel: Safe4DAppRegisterViewModel = viewModel(factory = Safe4DAppModule.FactoryRegister(
        Safe4DAppModule.RegisterInput()
    ))
) {
    val state by viewModel.registerState.collectAsState()
    val wallet = viewModel.wallet
    val sendResult = viewModel.sendResult
    val view = LocalView.current

    when (sendResult) {
        SendResult.Sending -> {
            HudHelper.showInProcessMessage(
                view,
                if (state.isEditing) R.string.Safe4_DApp_Update_Sending else R.string.Safe4_DApp_Register_Sending,
                SnackbarDuration.INDEFINITE
            )
        }

        is SendResult.Sent -> {
            HudHelper.showSuccessMessage(
                view,
                if (state.isEditing) R.string.Safe4_DApp_Update_Success else R.string.Safe4_DApp_Register_Success,
                SnackbarDuration.LONG
            )
            viewModel.sendResult = null
        }

        is SendResult.Failed -> {
            HudHelper.showErrorMessage(view, sendResult.caution.getString())
            viewModel.sendResult = null
        }

        null -> Unit
    }

    LaunchedEffect(sendResult) {
        if (sendResult is SendResult.Sent) {
            navController.popBackStack()
        }
    }

    HSScaffold(
        title = if (state.isEditing) {
            stringResource(R.string.Safe4_DApp_Update)
        } else {
            stringResource(R.string.Safe4_DApp_Register)
        },
        onBack = { navController.popBackStack() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // Contract Address
            if (wallet != null) {
                Column {
                    Text(
                        text = stringResource(R.string.Safe4_DApp_ContractAddr_Label),
                        style = ComposeAppTheme.typography.subheadB,
                        color = ComposeAppTheme.colors.leah,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    HSAddressInput(
                        modifier = Modifier.fillMaxWidth(),
                        initial = state.contractAddr.ifBlank { null }?.let { Address(it) },
                        tokenQuery = wallet.token.tokenQuery,
                        coinCode = wallet.coin.code,
                    ) {
                        viewModel.updateContractAddr(it)
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            // Name
            FormField(
                value = state.name,
                label = stringResource(R.string.Safe4_DApp_Name_Label),
                placeholder = stringResource(R.string.Safe4_DApp_Name_Placeholder),
                onValueChange = { viewModel.updateName(it) },
                error = state.nameError
            )

            Spacer(Modifier.height(16.dp))

            // URL
            FormField(
                value = state.url,
                label = stringResource(R.string.Safe4_DApp_URL_Label),
                placeholder = stringResource(R.string.Safe4_DApp_URL_Placeholder),
                onValueChange = { viewModel.updateUrl(it) }
            )

            Spacer(Modifier.height(16.dp))

            // Description
            FormField(
                value = state.description,
                label = stringResource(R.string.Safe4_DApp_Desc_Label),
                placeholder = stringResource(R.string.Safe4_DApp_Desc_Placeholder),
                onValueChange = { viewModel.updateDescription(it) },
                error = state.descError
            )

            Spacer(Modifier.height(16.dp))

            // Icon URL
            FormField(
                value = state.iconUrl,
                label = stringResource(R.string.Safe4_DApp_IconURL_Label),
                placeholder = stringResource(R.string.Safe4_DApp_IconURL_Placeholder),
                onValueChange = { viewModel.updateIconUrl(it) }
            )

            Spacer(Modifier.height(16.dp))

            // Official URL
            FormField(
                value = state.officialUrl,
                label = stringResource(R.string.Safe4_DApp_OfficialUrl_Label),
                placeholder = stringResource(R.string.Safe4_DApp_OfficialUrl_Placeholder),
                onValueChange = { viewModel.updateOfficialUrl(it) }
            )

            Spacer(Modifier.height(16.dp))

            // Official Email
            FormField(
                value = state.officialEmail,
                label = stringResource(R.string.Safe4_DApp_OfficialEmail_Label),
                placeholder = stringResource(R.string.Safe4_DApp_OfficialEmail_Placeholder),
                onValueChange = { viewModel.updateOfficialEmail(it) }
            )

            Spacer(Modifier.height(16.dp))

            // Official Account (Register Address) - only in edit mode
            if (state.isEditing && wallet != null) {
                Column {
                    Text(
                        text = stringResource(R.string.Safe4_DApp_OfficialAccount_Label),
                        style = ComposeAppTheme.typography.subheadB,
                        color = ComposeAppTheme.colors.leah,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    HSAddressInput(
                        modifier = Modifier.fillMaxWidth(),
                        initial = state.officialAccount.ifBlank { null }?.let { Address(it) },
                        tokenQuery = wallet.token.tokenQuery,
                        coinCode = wallet.coin.code,
                    ) {
                        viewModel.updateOfficialAccount(it)
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            // Keywords
            FormField(
                value = state.keyword,
                label = stringResource(R.string.Safe4_DApp_Keyword_Label),
                placeholder = stringResource(R.string.Safe4_DApp_Keyword_Placeholder),
                onValueChange = { viewModel.updateKeyword(it) }
            )

            // Keyword tags display
            if (state.keyword.isNotBlank()) {
                val keywords = state.keyword.split("|").filter { it.isNotBlank() }
                if (keywords.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        keywords.forEach { kw ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ComposeAppTheme.colors.elenaD)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = kw.trim(),
                                    style = ComposeAppTheme.typography.microSB,
                                    color = ComposeAppTheme.colors.jacob
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Logo section - only in edit mode
            if (state.isEditing) {
                LogoSections(viewModel)
                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(24.dp))

            // Submit button
            androidx.compose.material.Button(
                onClick = { viewModel.submit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = viewModel.canSubmit,
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material.ButtonDefaults.buttonColors(
                    backgroundColor = ComposeAppTheme.colors.jacob,
                    contentColor = ComposeAppTheme.colors.dark,
                    disabledBackgroundColor = ComposeAppTheme.colors.grey50,
                    disabledContentColor = ComposeAppTheme.colors.grey
                )
            ) {
                androidx.compose.material.Text(
                    text = if (state.isEditing) {
                        stringResource(R.string.Safe4_DApp_Update)
                    } else {
                        stringResource(R.string.Safe4_DApp_Submit)
                    },
                    style = ComposeAppTheme.typography.body,
                    color = ComposeAppTheme.colors.dark
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }

}

@Composable
private fun FormField(
    value: String,
    label: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    error: Int? = null
) {
    Column {
        Text(
            text = label,
            style = ComposeAppTheme.typography.subheadB,
            color = ComposeAppTheme.colors.leah,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ComposeAppTheme.colors.lawrence)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            androidx.compose.material.TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    androidx.compose.material.Text(
                        text = placeholder,
                        style = ComposeAppTheme.typography.body,
                        color = ComposeAppTheme.colors.grey50
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = ComposeAppTheme.typography.body.copy(
                    color = ComposeAppTheme.colors.leah
                ),
                singleLine = true,
                colors = androidx.compose.material.TextFieldDefaults.textFieldColors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    backgroundColor = Color.Transparent,
                    cursorColor = ComposeAppTheme.colors.jacob,
                    textColor = ComposeAppTheme.colors.leah,
                    disabledTextColor = ComposeAppTheme.colors.grey
                )
            )
        }

        if (error != null) {
            Text(
                text = stringResource(error),
                style = ComposeAppTheme.typography.caption,
                color = Color(0xFFFF3B30),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

@Composable
private fun LogoSections(
    viewModel: Safe4DAppRegisterViewModel
) {
    val context = LocalContext.current
    val view = LocalView.current
    val logoBytes = viewModel.logoBytes
    val logoPayAmount = viewModel.logoPayAmount
    val logoResult = viewModel.logoResult
    val oversizeMsg = stringResource(R.string.Safe4_DApp_Logo_Oversize)

    LaunchedEffect(Unit) {
        viewModel.loadLogoPayAmount()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val bytes = context.contentResolver.openInputStream(it)?.use { input ->
                    java.io.ByteArrayOutputStream().apply {
                        input.copyTo(this)
                    }.toByteArray()
                }
                if (bytes != null && bytes.size > 128 * 1024) {
                    HudHelper.showErrorMessage(view, oversizeMsg)
                } else if (bytes != null) {
                    viewModel.logoBytes = bytes
                }
            } catch (e: Exception) {
                HudHelper.showErrorMessage(view, e.message ?: "Failed to read image")
            }
        }
    }

    when (logoResult) {
        SendResult.Sending -> {
            HudHelper.showInProcessMessage(view, R.string.Safe4_DApp_Logo_Uploading, SnackbarDuration.INDEFINITE)
        }
        is SendResult.Sent -> {
            HudHelper.showSuccessMessage(view, R.string.Safe4_DApp_Logo_Success, SnackbarDuration.LONG)
            viewModel.logoResult = null
        }
        is SendResult.Failed -> {
            HudHelper.showErrorMessage(view, logoResult.caution.getString())
            viewModel.logoResult = null
        }
        null -> Unit
    }

    Column {
        Text(
            text = stringResource(R.string.Safe4_DApp_Logo_Label),
            style = ComposeAppTheme.typography.subheadB,
            color = ComposeAppTheme.colors.leah,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (logoPayAmount != null) {
            Text(
                text = stringResource(R.string.Safe4_DApp_Logo_Fee, logoPayAmount!!),
                style = ComposeAppTheme.typography.caption,
                color = ComposeAppTheme.colors.grey,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Preview area
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ComposeAppTheme.colors.lawrence),
                contentAlignment = Alignment.Center
            ) {
                if (logoBytes != null) {
                    androidx.compose.foundation.Image(
                        bitmap = BitmapFactory.decodeByteArray(logoBytes, 0, logoBytes.size)
                            .asImageBitmap(),
                        contentDescription = "Logo preview",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_gallery_24),
                        contentDescription = "No logo",
                        tint = ComposeAppTheme.colors.grey50
                    )
                }
            }

            // Pick image button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ComposeAppTheme.colors.lawrence)
                    .clickable {
                        galleryLauncher.launch("image/*")
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.Safe4_DApp_Logo_Pick),
                    style = ComposeAppTheme.typography.body,
                    color = ComposeAppTheme.colors.jacob
                )
            }

            // Upload button
            if (logoBytes != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ComposeAppTheme.colors.jacob)
                        .clickable {
                            viewModel.submitLogo()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.Safe4_DApp_Logo_Upload),
                        style = ComposeAppTheme.typography.body,
                        color = ComposeAppTheme.colors.dark
                    )
                }
            }
        }
    }
}
