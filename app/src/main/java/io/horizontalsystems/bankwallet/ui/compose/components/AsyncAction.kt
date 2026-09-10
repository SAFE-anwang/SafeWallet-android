package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Single-flight launcher for button clicks that start a coroutine.
 *
 * [run] disables re-entry until the running block completes, which prevents a
 * same-frame double tap from launching the work twice (the button's `enabled`
 * gate only updates on the next recomposition, so an in-handler guard is still
 * required). Use [inProgress] to drive the button's `enabled`/title.
 *
 * 内层 block 在 [NonCancellable] 上下文中执行，避免父协程取消（如 viewModelScope 销毁
 * 或返回时页面 popBackStack 触发的取消）导致发送中的交易被中断、抛出
 * "StandaloneCoroutine was cancelled" 错误。
 */
@Stable
class AsyncAction(private val scope: CoroutineScope) {
    var inProgress by mutableStateOf(false)
        private set

    fun run(block: suspend () -> Unit) {
        if (inProgress) return
        inProgress = true
        scope.launch {
            try {
                withContext(NonCancellable) {
                    block()
                }
            } catch (e: CancellationException) {
                // 显式取消静默处理
            } finally {
                inProgress = false
            }
        }
    }
}

@Composable
fun rememberAsyncAction(): AsyncAction {
    val scope = rememberCoroutineScope()
    return remember { AsyncAction(scope) }
}

