package io.horizontalsystems.bankwallet.modules.safe4

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.main.WebViewActivity
import io.horizontalsystems.bankwallet.modules.tg.StartTelegramsService
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSection
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.core.findNavController

class Safe4Fragment : BaseFragment() {

    private val viewModel by viewModels<Safe4ViewModel> { Safe4Module.Factory() }

    private var startTelegramService: StartTelegramsService? = null

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
                    Safe4Screen(viewModel, findNavController()) {
                        openLink(it)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startTelegramService?.stopCheckLoginStatus()
    }

    private fun openLink(url: String) {
        context?.let {
            when(url) {
                App.appConfigProvider.appTelegramLink -> {
                    joinTelegramGroup(url)
                }
                App.appConfigProvider.supportEmail -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(url)
                    requireActivity().startActivity(intent)
                }
                App.appConfigProvider.safeBSCPancakeswap,
                App.appConfigProvider.safeEthUniswap -> {
                    val bundle = Bundle()
                    bundle.putString("url", url)
                    bundle.putString("name", url.subSequence(8, url.length).toString())
                    findNavController().slideFromRight(R.id.dappBrowseFragment, bundle)
                }
                else -> {
                    startActivity(Intent(requireActivity(), WebViewActivity::class.java).apply {
                        putExtra("url", url)
                        putExtra("name", url.subSequence(8, url.length))
                    })
                }
            }
        }

    }

    private fun joinTelegramGroup(groupLink: String) {
        if (startTelegramService == null) {
            startTelegramService = StartTelegramsService(requireActivity())
        }
        startTelegramService?.join(groupLink)
    }
}

@Composable
private fun Safe4Screen(
    viewModel: Safe4ViewModel,
    navController: NavController,
    onClick: (String) -> Unit
) {

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                TranslatableString.ResString(R.string.Safe4_Title),
            )

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.height(16.dp))
                Safe4Sections(viewModel, navController, onClick)
            }
        }
    }
}

@Composable
private fun Safe4Sections(
    viewModel: Safe4ViewModel,
    navController: NavController,
    onClick: (String) -> Unit
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
            },
            {
                HsSettingCell(
                    R.string.Safe4_Safe_ETH_Contract,
                    R.mipmap.ic_app_color,
                    showAlert = false,
                    onClick = {
                        onClick(App.appConfigProvider.safeEthContract)
                    }
                )
            },
            {
                HsSettingCell(
                    R.string.Safe4_Safe_ETH_Uniswap,
                    R.mipmap.ic_app_color,
                    showAlert = false,
                    onClick = {
                        onClick(App.appConfigProvider.safeEthUniswap)
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
            },
            {
                HsSettingCell(
                    R.string.Safe4_Safe_BSC_Contract,
                    R.mipmap.ic_app_color,
                    showAlert = false,
                    onClick = {
                        onClick(App.appConfigProvider.safeBSCContract)
                    }
                )
            },
            {
                HsSettingCell(
                    R.string.Safe4_Safe_BSC_Pancakeswap,
                    R.mipmap.ic_app_color,
                    showAlert = false,
                    onClick = {
                        onClick(App.appConfigProvider.safeBSCPancakeswap)
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
                R.string.Safe4_Lock_Info,
                R.mipmap.ic_app_color,
                showAlert = false,
                onClick = {
                    if (!RepeatClickUtils.isRepeat) {
                        Safe4Module.handlerLineInfo()
                    }
                }
            )
        })
    )

    Spacer(Modifier.height(25.dp))

    Text(
        text = stringResource(R.string.Safe4_Safe_Basic_Info),
        style = ComposeAppTheme.typography.subhead1,
        color = ComposeAppTheme.colors.leah,
        maxLines = 1,
        modifier = Modifier.padding(horizontal = 16.dp)
    )

    Spacer(Modifier.height(10.dp))

    CellSingleLineLawrenceSection(
        listOf ({
            HsSettingCell(
                R.string.Safe4_Safe_Homepage,
                R.mipmap.ic_app_color,
                showAlert = false,
                onClick = {
                    onClick(App.appConfigProvider.appWebPageLink)
                }
            )
        },{
            HsSettingCell(
                R.string.Safe4_Safe_Block_Explorer,
                R.mipmap.ic_app_color,
                showAlert = false,
                onClick = {
                    onClick(App.appConfigProvider.safeBlockExplorer)
                }
            )
        },{
            HsSettingCell(
                R.string.Safe4_Safe_Across_Chain_Explorer,
                R.mipmap.ic_app_color,
                showAlert = false,
                onClick = {
                    onClick(App.appConfigProvider.safeAcrossChainExplorer)
                }
            )
        },{
            HsSettingCell(
                R.string.Safe4_Safe_Coingecko,
                R.mipmap.ic_app_color,
                showAlert = false,
                onClick = {
                    onClick(App.appConfigProvider.safeCoinGecko)
                }
            )
        },{
            HsSettingCell(
                    R.string.Safe4_Safe_coinmarketcap,
                    R.mipmap.ic_app_color,
                    showAlert = false,
                    onClick = {
                        onClick(App.appConfigProvider.safeCoinMarketCap)
                    }
            )
        },{
            HsSettingCell(
                R.string.Safe4_Safe_BEP20_Coingecko,
                R.mipmap.ic_app_color,
                showAlert = false,
                onClick = {
                    onClick(App.appConfigProvider.safeSafeBEP20)
                }
            )
        })
    )

    Spacer(Modifier.height(32.dp))

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

