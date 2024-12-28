package io.horizontalsystems.bankwallet.modules.receive.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.UsedAddress
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.bankwallet.ui.compose.components.Tabs
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_issykBlue
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import java.math.BigInteger

data class MoreAddressesParams(
        val coinName: String,
        val moreAddresses: List<MoreAddressInfo>
)

data class AddressData(
        val address: String,
        val privateKey: BigInteger,
        val balance: BigInteger
)

data class MoreAddressInfo(
        val address: String,
        val privateKey: String,
        val balance: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MoreAddressScreen(
        params: List<MoreAddressInfo>,
        onBackPress: () -> Unit
) {


    val view = LocalView.current
    Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                        title = stringResource(id = R.string.Balance_Receive_MoreAddresses),
                        navigationIcon = {
                            HsBackButton(onClick = onBackPress)
                        }
                )
            }
    ) {
        Column(
                modifier = Modifier
                        .padding(it)
                        .padding(start = 16.dp, end = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ComposeAppTheme.colors.lawrence)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
        ) {

            VSpacer(12.dp)
            for (item in params){
                Column(
                        modifier = Modifier
                                .padding(horizontal = 16.dp)) {
                    Row {

                        subhead2_grey(
                                modifier = Modifier.weight(1f),
                                text = stringResource(id = R.string.AnBao_Addresses))
                        subhead2_leah(
                                modifier = Modifier.weight(5f),
                                text = item.address)
                    }

                    HSpacer(16.dp)

                    Row(modifier = Modifier.clickable {
                        TextHelper.copyText(item.privateKey)
                        HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                    }) {

                        subhead2_grey(
                                modifier = Modifier.weight(1f),
                                text = stringResource(id = R.string.AnBao_Private_Key))
                        subhead2_issykBlue(
                                modifier = Modifier.weight(5f),
                                text = item.privateKey
                        )
                    }
                    HSpacer(16.dp)

                    Row {

                        subhead2_grey(
                                modifier = Modifier.weight(1f),
                                text = stringResource(id = R.string.AnBao_Balance))
                        subhead2_leah(
                                modifier = Modifier.weight(5f),
                                text = item.balance
                        )
                    }
                }
                VSpacer(12.dp)
                Divider(
                        thickness = 1.dp,
                        color = ComposeAppTheme.colors.steel10,
                        modifier = Modifier.fillMaxWidth()
                )
                VSpacer(12.dp)
            }

        }
    }
}
