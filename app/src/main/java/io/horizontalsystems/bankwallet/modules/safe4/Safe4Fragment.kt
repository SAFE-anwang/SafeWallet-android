package io.horizontalsystems.bankwallet.modules.safe4

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSection
import io.horizontalsystems.core.findNavController

class Safe4Fragment : BaseFragment() {

    private val viewModel by viewModels<Safe4ViewModel> { Safe4Module.Factory() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    Safe4Screen(viewModel, findNavController())
                }
            }
        }
    }

}

@Composable
private fun Safe4Screen(
    viewModel: Safe4ViewModel,
    navController: NavController
) {

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                TranslatableString.ResString(R.string.Safe4_Title),
            )

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.height(16.dp))
                Safe4Sections(viewModel, navController)
            }
        }
    }
}

@Composable
private fun Safe4Sections(
    viewModel: Safe4ViewModel,
    navController: NavController
) {

    Text(
        text = stringResource(R.string.Safe4_Cross_Chain_erc20),
        style = ComposeAppTheme.typography.subhead1,
        color = ComposeAppTheme.colors.leah,
        maxLines = 1,
        modifier = Modifier.padding(horizontal = 16.dp)
    )

    Spacer(Modifier.height(10.dp))

    CellSingleLineLawrenceSection(
        listOf (
            {
                HsSettingCellForEth(
                    R.mipmap.ic_app_color,
                    "SAFE",
                    "ERC20",
                    onClick = {
                        if (!RepeatClickUtils.isRepeat) {
                            Safe4Module.handlerSafe2eth(Safe4Module.ChainType.ETH)
                        }
                    }
                )
            },
            {
                HsSettingCellForSafe(
                    R.mipmap.ic_app_color,
                    "SAFE",
                    "ERC20",
                    onClick = {
                        Safe4Module.handlerEth2safe(Safe4Module.ChainType.ETH, navController)
                    }
                )
            }
        )
    )

    Spacer(Modifier.height(25.dp))

    Text(
        text = stringResource(R.string.Safe4_Cross_Chain_bep20),
        style = ComposeAppTheme.typography.subhead1,
        color = ComposeAppTheme.colors.leah,
        maxLines = 1,
        modifier = Modifier.padding(horizontal = 16.dp)
    )

    Spacer(Modifier.height(10.dp))

    CellSingleLineLawrenceSection(
        listOf (
            {
                HsSettingCellForEth(
                    R.mipmap.ic_app_color,
                    "SAFE",
                    "BEP20",
                    onClick = {
                        Safe4Module.handlerSafe2eth(Safe4Module.ChainType.BSC)
                    }
                )
            },
            {
                HsSettingCellForSafe(
                    R.mipmap.ic_app_color,
                    "SAFE",
                    "BEP20",
                    onClick = {
                        Safe4Module.handlerEth2safe(Safe4Module.ChainType.BSC, navController)
                    }
                )
            }
        )
    )

    Spacer(Modifier.height(25.dp))

    Text(
        text = stringResource(R.string.Safe4_Locked),
        style = ComposeAppTheme.typography.subhead1,
        color = ComposeAppTheme.colors.leah,
        maxLines = 1,
        modifier = Modifier.padding(horizontal = 16.dp)
    )

    Spacer(Modifier.height(10.dp))

    CellSingleLineLawrenceSection(
        listOf ({
            HsSettingCell(
                R.string.Safe4_Line_Locked,
                R.mipmap.ic_app_color,
                showAlert = false,
                onClick = {
                    if (!RepeatClickUtils.isRepeat) {
                        Safe4Module.handlerLineLock()
                    }
                }
            )
        },{
            HsSettingCell(
                R.string.Safe4_node_Locked,
                R.mipmap.ic_app_color,
                showAlert = false,
                onClick = {
                    Toast.makeText(App.instance,
                        Translator.getString(R.string.Safe4_Coming_Soon), Toast.LENGTH_SHORT).show()
                }
            )
        })
    )

    Spacer(Modifier.height(25.dp))

}

@Composable
fun HsSettingCell(
    @StringRes title: Int,
    @DrawableRes icon: Int,
    value: String? = null,
    showAlert: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .clickable(onClick = { onClick.invoke() }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = icon),
            contentDescription = null,
        )
        Text(
            text = stringResource(title),
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.weight(1f))
        value?.let {
            Text(
                text = it,
                style = ComposeAppTheme.typography.subhead1,
                color = ComposeAppTheme.colors.leah,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        if (showAlert) {
            Image(
                modifier = Modifier.size(20.dp),
                painter = painterResource(id = R.drawable.ic_attention_red_20),
                contentDescription = null,
            )
            Spacer(Modifier.width(12.dp))
        }
        Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
        )
    }

}


@Composable
fun HsSettingCellForEth(
    @DrawableRes icon: Int,
    coinName: String,
    chainName: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .clickable(onClick = { onClick.invoke() }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = icon),
            contentDescription = null,
        )
        Text(
            text = coinName,
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah,
            maxLines = 1,
            modifier = Modifier.padding(start = 16.dp)
        )
        Text(
            text = "=>",
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        Text(
            text = coinName,
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah,
            maxLines = 1,
            modifier = Modifier.padding(start = 2.dp)
        )
        Box(
            modifier = Modifier
                .padding(start = 6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ComposeAppTheme.colors.jeremy)
        ) {
            Text(
                modifier = Modifier.padding(
                    start = 4.dp,
                    end = 4.dp,
                    bottom = 1.dp
                ),
                text = chainName,
                color = ComposeAppTheme.colors.bran,
                style = ComposeAppTheme.typography.microSB,
                maxLines = 1,
            )
        }
        Spacer(Modifier.weight(1f))

        Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
        )
    }
}


@Composable
fun HsSettingCellForSafe(
    @DrawableRes icon: Int,
    coinName: String,
    chainName: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .clickable(onClick = { onClick.invoke() }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = icon),
            contentDescription = null,
        )
        Text(
            text = coinName,
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah,
            maxLines = 1,
            modifier = Modifier.padding(start = 16.dp)
        )
        Box(
            modifier = Modifier
                .padding(start = 6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ComposeAppTheme.colors.jeremy)
        ) {
            Text(
                modifier = Modifier.padding(
                    start = 4.dp,
                    end = 4.dp,
                    bottom = 1.dp
                ),
                text = chainName,
                color = ComposeAppTheme.colors.bran,
                style = ComposeAppTheme.typography.microSB,
                maxLines = 1,
            )
        }
        Text(
            text = "=>",
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        Text(
            text = coinName,
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah,
            maxLines = 1,
            modifier = Modifier.padding(end = 6.dp)
        )
        Spacer(Modifier.weight(1f))

        Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
        )
    }
}

