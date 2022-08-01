package io.horizontalsystems.bankwallet.modules.send.submodules.hodler

import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stringResId
import io.horizontalsystems.bankwallet.databinding.ViewHodlerInputBinding
import io.horizontalsystems.bankwallet.modules.safe4.linelock.LineLockSendHandler
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.SendSubmoduleFragment
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem

class SendHodlerFragment(
    private val hodlerModuleDelegate: SendHodlerModule.IHodlerModuleDelegate,
    private val sendHandler: SendModule.ISendHandler
) : SendSubmoduleFragment() {

    private val presenter by activityViewModels<SendHodlerPresenter> {
        SendHodlerModule.Factory(
            sendHandler,
            hodlerModuleDelegate
        )
    }

    private var _binding: ViewHodlerInputBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ViewHodlerInputBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val presenterView = presenter.view as SendHodlerView

        if (sendHandler is LineLockSendHandler) {
            binding.lockLayout.visibility = View.GONE
            binding.lineLockLayout.visibility = View.VISIBLE
        } else {
            binding.lockLayout.visibility = View.VISIBLE
            binding.lineLockLayout.visibility = View.GONE
        }

        binding.lockLayout.setOnClickListener {
            presenter.onClickLockTimeInterval()
        }

        binding.lockedValue.editText?.filters = arrayOf<InputFilter>(PointInputFilter())
        binding.lockedValue.editText?.addTextChangedListener(afterTextChanged = {
            presenter.onTextChangeLockedValue(it.toString())
        })

        binding.startMonth.editText?.addTextChangedListener(afterTextChanged = {
            presenter.onTextChangeStartMonth(it.toString())
        })

        binding.intervalMonth.editText?.addTextChangedListener(afterTextChanged = {
            presenter.onTextChangeIntervalMonth(it.toString())
        })

        presenterView.lockedValueEvent.observe(viewLifecycleOwner, Observer {
            binding.lockedValue.editText?.setText(it)
        })

        presenterView.startMonthEvent.observe(viewLifecycleOwner, Observer {
            binding.startMonth.editText?.setText(it)
        })

        presenterView.intervalMonthEvent.observe(viewLifecycleOwner, Observer {
            binding.intervalMonth.editText?.setText(it)
        })

        presenterView.selectedLockTimeInterval.observe(viewLifecycleOwner, Observer {
            binding.lockTimeMenu.setText(it.stringResId())
        })

        presenterView.showLockTimeIntervals.observe(viewLifecycleOwner, Observer { lockTimeIntervals ->
            val selectorItems = lockTimeIntervals.map {
                SelectorItem(getString(it.lockTimeInterval.stringResId()), it.selected)
            }
            SelectorDialog
                .newInstance(selectorItems, getString(R.string.Send_DialogLockTime), { position ->
                    presenter.onSelectLockTimeInterval(position)
                })
                .show(this.parentFragmentManager, "time_intervals_selector")

        })
    }

    override fun init() {
        presenter.onViewDidLoad()
    }

}
