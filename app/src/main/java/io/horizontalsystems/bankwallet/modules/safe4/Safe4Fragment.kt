package io.horizontalsystems.bankwallet.modules.safe4

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
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
                    Safe4Screen(viewModel, requireActivity(), findNavController())
                }
            }
        }
    }

}

@Composable
private fun Safe4Screen(
    viewModel: Safe4ViewModel,
    activity: Activity,
    navController: NavController
) {

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                TranslatableString.ResString(R.string.Safe4_Title),
            )

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.height(16.dp))
                Safe4Sections(viewModel, activity, navController)
            }
        }
    }
}

@Composable
private fun Safe4Sections(
    viewModel: Safe4ViewModel,
    activity: Activity,
    navController: NavController
) {

    Text(
        text = stringResource(R.string.Safe4_Cross_Chain),
        style = ComposeAppTheme.typography.subhead1,
        color = ComposeAppTheme.colors.leah,
        maxLines = 1,
        modifier = Modifier.padding(horizontal = 16.dp)
    )

    Spacer(Modifier.height(10.dp))

    CellSingleLineLawrenceSection(
        listOf ({
            HsSettingCell(
                R.string.Safe4_Title_safe2wsafe,
                R.mipmap.ic_app_color,
                showAlert = false,
                onClick = {
                    Safe4Module.startSafe2wsafe(activity);
                }
            )
        },{
            HsSettingCell(
                R.string.Safe4_Title_wsafe2safe,
                R.mipmap.ic_app_color,
                showAlert = false,
                onClick = {
                    Safe4Module.startWsafe2Safe(activity, navController)
                }
            )
        })
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
                    Toast.makeText(activity, "敬请期待", Toast.LENGTH_SHORT).show()
                }
            )
        },{
            HsSettingCell(
                R.string.Safe4_node_Locked,
                R.mipmap.ic_app_color,
                showAlert = false,
                onClick = {
                    Toast.makeText(activity, "敬请期待", Toast.LENGTH_SHORT).show()
                }
            )
        })
    )

    Spacer(Modifier.height(25.dp))

    Text(
        text = stringResource(R.string.Safe4_Defi),
        style = ComposeAppTheme.typography.subhead1,
        color = ComposeAppTheme.colors.leah,
        maxLines = 1,
        modifier = Modifier.padding(horizontal = 16.dp)
    )

    Spacer(Modifier.height(10.dp))

    CellSingleLineLawrenceSection(
        listOf ({
            HsSettingCell(
                R.string.Safe4_Defi_uniswap,
                R.mipmap.ic_app_color,
                showAlert = false,
                onClick = {
                    Toast.makeText(activity, "敬请期待", Toast.LENGTH_SHORT).show()
                }
            )
        },{
            HsSettingCell(
                R.string.Safe4_Defi_1inch,
                R.mipmap.ic_app_color,
                showAlert = false,
                onClick = {
                    Toast.makeText(activity, "敬请期待", Toast.LENGTH_SHORT).show()
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

