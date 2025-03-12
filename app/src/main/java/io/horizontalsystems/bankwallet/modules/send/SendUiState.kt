package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.hodler.LockTimeInterval
import java.math.BigDecimal


data class SendUiState(
        val availableBalance: BigDecimal,
        val amountCaution: HSCaution?,
        val addressError: Throwable?,
        val canBeSend: Boolean,
        val showAddressInput: Boolean,
        val lockTimeInterval: LockTimeInterval? = null,
        val lockAmountError: Boolean = false
)
