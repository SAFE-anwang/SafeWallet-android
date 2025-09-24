package io.horizontalsystems.bankwallet.modules.safe4.node.withdraw

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow2
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsCheckbox
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.core.helpers.HudHelper

object WithdrawUi {

    @Composable
    fun WithdrawItem(
        lockId: Long,
        amount: String,
        enable: Boolean,
        checked: Boolean,
        unHeight: Long?,
        releaseHeight: Long? = null,
        address: String? = null,
        onChecked: (Long, Boolean) -> Unit
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(
                    1.dp,
                    ComposeAppTheme.colors.lawrence,
                    RoundedCornerShape(8.dp)
                )
                .background(ComposeAppTheme.colors.lawrence)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column  {
                    body_bran(
                        text = stringResource(R.string.SAFE4_Withdraw_Lock_Id, lockId)
                    )
                    VSpacer(4.dp)
                    body_bran(
                        text = stringResource(R.string.SAFE4_Withdraw_Lock_Amount, amount)
                    )
                    unHeight?.let {
                        VSpacer(4.dp)
                        body_bran(
                            text = stringResource(R.string.SAFE4_Withdraw_Unlock_Height, it)
                        )
                    }
                    releaseHeight?.let {
                        VSpacer(4.dp)
                        body_bran(
                            text = stringResource(R.string.SAFE4_Withdraw_Release_Height, it)
                        )
                    }
                    address?.let {
                        VSpacer(4.dp)
                        body_bran(
                            text = stringResource(R.string.SAFE4_Withdraw_Address, it.shorten(8))
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(
                ) {
                    HsCheckbox(
                        checked,
                        enable
                    ) {
                        onChecked.invoke(lockId, it)
                    }
                }
            }
        }
        VSpacer(3.dp)
    }

    @Composable
    fun WithdrawLockItem(
        lockId: Long,
        amount: String,
        withdrawEnable: Boolean,
        addLockDayEnable: Boolean?,
        unlockHeight: Long?,
        releaseHeight: Long?,
        address: String?,
        address2: String?,
        onWithdraw: () -> Unit,
        onAddLockDay: (Long) -> Unit
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(
                    1.dp,
                    ComposeAppTheme.colors.lawrence,
                    RoundedCornerShape(8.dp)
                )
                .background(ComposeAppTheme.colors.lawrence)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column  {
                    body_bran(
                        text = stringResource(R.string.SAFE4_Withdraw_Lock_Id, lockId)
                    )
                    VSpacer(4.dp)
                    body_bran(
                        text = stringResource(R.string.SAFE4_Withdraw_Lock_Amount, amount)
                    )
                    unlockHeight?.let {
                        VSpacer(4.dp)
                        body_bran(
                            text = stringResource(R.string.SAFE4_Withdraw_Unlock_Height, it)
                        )
                    }
                    releaseHeight?.let {
                        if (it != 0L) {
                            VSpacer(4.dp)
                            body_bran(
                                text = stringResource(R.string.SAFE4_Withdraw_Release_Height, it)
                            )
                        }
                    }
                    address?.let {
                        VSpacer(4.dp)
                        body_bran(
                            text = stringResource(R.string.SAFE4_Withdraw_Address, it.shorten(8))
                        )
                    }
                    /*address2?.let {
                        VSpacer(4.dp)
                        body_bran(
                            text = stringResource(R.string.SAFE4_Withdraw_Address, it.shorten(8))
                        )
                    }*/
                    Row {
                        ButtonPrimaryYellow2(
                            modifier = Modifier.weight(1f).height(25.dp),
                            title = stringResource(R.string.SAFE4_Withdraw_Release),
                            onClick = {
                                onWithdraw.invoke()
                            },
                            enabled = withdrawEnable
                        )
                        addLockDayEnable?.let {
                            HSpacer(16.dp)
                            ButtonPrimaryYellow2(
                                modifier = Modifier.weight(1f).height(25.dp),
                                title = stringResource(R.string.Safe_Four_Node_Add_Lock_Day),
                                onClick = {
                                    onAddLockDay.invoke(lockId)
                                },
                                enabled = addLockDayEnable
                            )
                        }
                    }
                }

            }
        }
        VSpacer(3.dp)
    }

}