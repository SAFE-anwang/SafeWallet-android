package io.horizontalsystems.bankwallet.ui.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.SwipeRefreshState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.theme.ThemeType

@Composable
fun HSSwipeRefresh(
    state: SwipeRefreshState,
    onRefresh: () -> Unit,
    swipeEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    SwipeRefresh(
        modifier = Modifier.fillMaxSize(),
        state = state,
        onRefresh = onRefresh,
        swipeEnabled = swipeEnabled,
        indicator = { swipeRefreshState, trigger ->
            SwipeRefreshIndicator(
                state = swipeRefreshState,
                refreshTriggerDistance = trigger,
                scale = true,
                backgroundColor = if (App.localStorage.currentTheme == ThemeType.Blue) ComposeAppTheme.colors.lawrence else ComposeAppTheme.colors.claude,
                contentColor = ComposeAppTheme.colors.leah,
            )
        },
        content = content
    )
}
