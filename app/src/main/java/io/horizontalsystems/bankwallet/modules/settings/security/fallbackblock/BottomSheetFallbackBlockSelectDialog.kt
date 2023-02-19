package io.horizontalsystems.bankwallet.modules.settings.security.fallbackblock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSectionFramed
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader

class BottomSheetFallbackBlockSelectDialog() : BaseComposableBottomSheetFragment() {

    var items: List<FallbackBlockViewModel.FallbackTimeViewItem>? = null
    var selectedItem: FallbackBlockViewModel.FallbackTimeViewItem? = null
    var onSelectListener: ((FallbackBlockViewModel.FallbackTimeViewItem) -> Unit)? = null

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
                    BottomSheetScreen(
                        swapProviders = items,
                        selectedItem = selectedItem,
                        onSelectListener = onSelectListener,
                        onCloseClick = { close() }
                    )
                }
            }
        }
    }

}

@Composable
private fun BottomSheetScreen(
    swapProviders: List<FallbackBlockViewModel.FallbackTimeViewItem>?,
    selectedItem: FallbackBlockViewModel.FallbackTimeViewItem?,
    onSelectListener: ((FallbackBlockViewModel.FallbackTimeViewItem) -> Unit)?,
    onCloseClick: () -> Unit
) {
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_back),
        title = stringResource(R.string.fallback_block),
        onCloseClick = onCloseClick,
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob)
    ) {
        Spacer(Modifier.height(12.dp))
        swapProviders?.let { items ->
            CellSingleLineLawrenceSectionFramed(items) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            onSelectListener?.invoke(item)
                            onCloseClick.invoke()
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(16.dp))
                    body_leah(text = stringResource(item.nameRes))
                    Spacer(modifier = Modifier.weight(1f))
                    if (item == selectedItem) {
                        Image(
                            modifier = Modifier.padding(start = 5.dp),
                            painter = painterResource(id = R.drawable.ic_checkmark_20),
                            colorFilter = ColorFilter.tint(ComposeAppTheme.colors.jacob),
                            contentDescription = null
                        )
                    }

                }
            }
        }
        Spacer(Modifier.height(44.dp))
    }
}

