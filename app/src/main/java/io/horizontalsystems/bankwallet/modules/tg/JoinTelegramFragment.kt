package io.horizontalsystems.bankwallet.modules.tg

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.exoplayer2.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.balance.ui.BalanceScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryBlue
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.core.findNavController
import kotlinx.coroutines.*
import org.telegram.messenger.UserConfig
import org.telegram.ui.LaunchActivity

class JoinTelegramFragment: BaseFragment() {

    private var checkTelegramLoginState = false

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
                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(painter = painterResource(id = R.drawable.ic_telegram_logo), contentDescription = null,
                            modifier = Modifier.width(100.dp).height(100.dp))
                            Spacer(modifier = Modifier.height(26.dp))
                            Text(
                                modifier = Modifier.padding(horizontal = 48.dp),
                                text = stringResource(id = R.string.Telegram_Join_Group),
                                style = ComposeAppTheme.typography.subhead2,
                                color = ComposeAppTheme.colors.grey,
                                textAlign = TextAlign.Center,
                            )
                        }

                        Spacer(modifier = Modifier.height(48.dp))
                        ButtonPrimaryBlue(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            title = stringResource(R.string.Join_Telegram_Group1),
                            onClick = {
                                join("https://t.me/safeanwang")
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        ButtonPrimaryBlue(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            title = stringResource(R.string.Join_Telegram_Group2),
                            onClick = {
                                join("https://t.me/safewallet2022")
                            }
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    private fun join(group: String) {
        val intent = Intent(context, LaunchActivity::class.java)
        intent.action = Intent.ACTION_VIEW
        intent.data = Uri.parse(group)
        requireActivity().startActivity(intent)
        startCheckLoginState(group)
    }

    private fun startCheckLoginState(group: String) {
        if (UserConfig.getActivatedAccountsCount() > 0) return
        checkTelegramLoginState = true
        GlobalScope.launch(Dispatchers.IO) {
            while (checkTelegramLoginState) {
                if (UserConfig.getActivatedAccountsCount() > 0) {
                    checkTelegramLoginState = false
                    withContext(Dispatchers.Main) {
                        join(group)
                    }
                    break
                }
                delay(2000)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkTelegramLoginState = false
    }
}