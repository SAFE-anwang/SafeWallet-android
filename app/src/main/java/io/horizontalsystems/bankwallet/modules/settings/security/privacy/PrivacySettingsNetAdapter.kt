package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.databinding.ViewHolderNetControlBinding
import io.horizontalsystems.bankwallet.modules.settings.security.tor.TorStatus


class PrivacySettingsNetAdapter(val context: Context, private val listener: Listener) : RecyclerView.Adapter<PrivacySettingsNetAdapter.NetControlViewHolder>() {

    interface Listener {
        fun onTorSwitchChecked(checked: Boolean)
        fun onVpnSwitchChecked(checked: Boolean)
    }


    private val sp = context.getSharedPreferences("vpnSetting", Context.MODE_PRIVATE)
    private var vpnChecked = sp.getBoolean("vpnOpen", true)

    private var torStatus: TorStatus = TorStatus.Closed
    private var checked = false

    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetControlViewHolder {
        return NetControlViewHolder(
            ViewHolderNetControlBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                {  isChecked ->
                    if (isChecked && vpnChecked) {
                        vpnChecked = false
                        saveVpnStatus()
                        listener.onVpnSwitchChecked(false)
                    }
                    listener.onTorSwitchChecked(isChecked)
                },
                { isChecked ->
                    // 连接了TOR网络，连接VPN时，需要断开
                    if (isChecked && checked) {
                        listener.onTorSwitchChecked(false)
                        checked = false
                    }
                    vpnChecked = isChecked
                    saveVpnStatus()
                    listener.onVpnSwitchChecked(isChecked)
                    setTorSwitch(checked)
                })
    }

    private fun saveVpnStatus() {
        sp.edit().putBoolean("vpnOpen", vpnChecked).commit()
    }

    override fun onBindViewHolder(holder: NetControlViewHolder, position: Int) {
        holder.bind(torStatus, checked, vpnChecked)
    }

    fun setTorSwitch(checked: Boolean){
        this.checked = checked
        notifyItemChanged(0)
    }

    fun bind(torStatus: TorStatus) {
        this.torStatus = torStatus
        notifyItemChanged(0)
    }

    class NetControlViewHolder(
        private val binding: ViewHolderNetControlBinding,
        private val onTorSwitch: (isChecked: Boolean) -> Unit,
        private val vpnOnSwitch: (isChecked: Boolean) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.torControlView.setOnClickListener {
                binding.torConnectionSwitch.isChecked = !binding.torConnectionSwitch.isChecked
            }
            binding.vpnControlView.setOnClickListener {
                binding.vpnConnectionSwitch.isChecked = !binding.vpnConnectionSwitch.isChecked
            }
        }

        fun bind(torStatus: TorStatus, checked: Boolean, vpnChecked: Boolean) {

            binding.torConnectionSwitch.setOnCheckedChangeListener(null)
            binding.torConnectionSwitch.isChecked = checked

            binding.vpnConnectionSwitch.setOnCheckedChangeListener(null)
            binding.vpnConnectionSwitch.isChecked = vpnChecked


            binding.torConnectionSwitch.setOnCheckedChangeListener { _, isChecked ->
                onTorSwitch.invoke(isChecked)
            }
            binding.vpnConnectionSwitch.setOnCheckedChangeListener { _, isChecked ->
                vpnOnSwitch.invoke(isChecked)
            }

            if (vpnChecked) {
                binding.vpnSubtitleText.text = binding.wrapper.context.getString(R.string.TorPage_Connected)
            } else {
                binding.vpnSubtitleText.text = binding.wrapper.context.getString(R.string.TorPage_ConnectionClosed)
            }

            when (torStatus) {
                TorStatus.Connecting -> {
                    binding.connectionSpinner.isVisible = true
                    binding.controlIcon.setImageDrawable(null)
                    binding.subtitleText.text = binding.wrapper.context.getString(R.string.TorPage_Connecting)
                }
                TorStatus.Connected -> {
                    binding.connectionSpinner.isVisible = false
                    binding.controlIcon.imageTintList = getTint(R.color.yellow_d)
                    binding.controlIcon.setImageResource(R.drawable.ic_tor_connection_success_24)
                    binding.subtitleText.text = binding.wrapper.context.getString(R.string.TorPage_Connected)
                }
                TorStatus.Failed -> {
                    binding.connectionSpinner.isVisible = false
                    binding.controlIcon.imageTintList = getTint(R.color.yellow_d)
                    binding.controlIcon.setImageResource(R.drawable.ic_tor_connection_error_24)
                    binding.subtitleText.text = binding.wrapper.context.getString(R.string.TorPage_Failed)
                }
                TorStatus.Closed -> {
                    binding.connectionSpinner.isVisible = false
                    binding.controlIcon.imageTintList = getTint(R.color.yellow_d)
                    binding.controlIcon.setImageResource(R.drawable.ic_tor_connection_24)
                    binding.subtitleText.text = binding.wrapper.context.getString(R.string.TorPage_ConnectionClosed)
                }
            }

        }

        private fun getTint(color: Int) = binding.wrapper.context?.let {
            ColorStateList.valueOf(ContextCompat.getColor(it, color))
        }

    }
}
