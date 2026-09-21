package io.horizontalsystems.bankwallet.core

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class ViewModelUiState<T> : ViewModel() {

    private val _uiState by lazy {
        mutableStateOf(createState())
    }

    val uiState: T
        get() = _uiState.value

    protected abstract fun createState() : T

    protected fun emitState() {
        viewModelScope.launch {
            _uiState.value = createState()
        }
    }

    /**
     * 在 IO 线程构建状态后回主线程赋值。
     *
     * 适用于 createState() 计算量较大的场景（如遍历大列表、排序、聚合），
     * 避免高频触发（如搜索输入）时阻塞主线程造成卡顿。
     */
    protected fun emitStateOnIO() {
        viewModelScope.launch(Dispatchers.IO) {
            val state = createState()
            withContext(Dispatchers.Main) {
                _uiState.value = state
            }
        }
    }
}
