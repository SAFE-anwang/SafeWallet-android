package io.horizontalsystems.bankwallet.modules.send.submodules

import androidx.fragment.app.Fragment
import io.horizontalsystems.bankwallet.core.BaseFragment

abstract class SendSubmoduleFragment : BaseFragment() {
    abstract fun init()
}
