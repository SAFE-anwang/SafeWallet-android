package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.TorStatus
import io.horizontalsystems.core.helpers.LocaleHelper
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_net_control.*


class PrivacySettingsNetAdapter(val context: Context, private val listener: Listener) : RecyclerView.Adapter<PrivacySettingsNetAdapter.TorControlViewHolder>() {

    interface Listener {
        fun onTorSwitchChecked(checked: Boolean)
        fun onVpnSwitchChecked(checked: Boolean)
    }


    private val sp = context.getSharedPreferences("vpnSetting", Context.MODE_PRIVATE)
    private var vpnChecked = sp.getBoolean("vpnOpen", true)

    private var torStatus: TorStatus = TorStatus.Closed
    private var checked = false

    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TorControlViewHolder {
        return TorControlViewHolder.create(parent, onTorSwitch = { isChecked ->
            if (isChecked && vpnChecked) {
                vpnChecked = false
                saveVpnStatus()
                listener.onVpnSwitchChecked(false)
            }
            listener.onTorSwitchChecked(isChecked)
        },
        onVpnSwitch = { isChecked ->
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

    override fun onBindViewHolder(holder: TorControlViewHolder, position: Int) {
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

    class TorControlViewHolder(
            override val containerView: View,
            private val onTorSwitch: (isChecked: Boolean) -> Unit,
            private val vpnOnSwitch: (isChecked: Boolean) -> Unit
    ) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        init {
            torControlView.setOnClickListener {
                torConnectionSwitch.isChecked = !torConnectionSwitch.isChecked
            }
            vpnControlView.setOnClickListener {
                vpnConnectionSwitch.isChecked = !vpnConnectionSwitch.isChecked
            }
        }

        fun bind(torStatus: TorStatus, checked: Boolean, vpnChecked: Boolean) {

            torConnectionSwitch.setOnCheckedChangeListener(null)
            torConnectionSwitch.isChecked = checked

            vpnConnectionSwitch.setOnCheckedChangeListener(null)
            vpnConnectionSwitch.isChecked = vpnChecked


            torConnectionSwitch.setOnCheckedChangeListener { _, isChecked ->
                onTorSwitch.invoke(isChecked)
            }
            vpnConnectionSwitch.setOnCheckedChangeListener { _, isChecked ->
                vpnOnSwitch.invoke(isChecked)
            }

            if (vpnChecked) {
                vpnSubtitleText.text = containerView.context.getString(R.string.TorPage_Connected)
            } else {
                vpnSubtitleText.text = containerView.context.getString(R.string.TorPage_ConnectionClosed)
            }

            when (torStatus) {
                TorStatus.Connecting -> {
                    connectionSpinner.isVisible = true
                    controlIcon.setImageDrawable(null)
                    subtitleText.text = containerView.context.getString(R.string.TorPage_Connecting)
                }
                TorStatus.Connected -> {
                    connectionSpinner.isVisible = false
                    controlIcon.imageTintList = getTint(R.color.yellow_d)
                    controlIcon.setImageResource(R.drawable.ic_tor_connection_success_24)
                    subtitleText.text = containerView.context.getString(R.string.TorPage_Connected)
                }
                TorStatus.Failed -> {
                    connectionSpinner.isVisible = false
                    controlIcon.imageTintList = getTint(R.color.yellow_d)
                    controlIcon.setImageResource(R.drawable.ic_tor_connection_error_24)
                    subtitleText.text = containerView.context.getString(R.string.TorPage_Failed)
                }
                TorStatus.Closed -> {
                    connectionSpinner.isVisible = false
                    controlIcon.imageTintList = getTint(R.color.yellow_d)
                    controlIcon.setImageResource(R.drawable.ic_tor_connection_24)
                    subtitleText.text = containerView.context.getString(R.string.TorPage_ConnectionClosed)
                }
            }

        }

        private fun getTint(color: Int) = containerView.context?.let { ColorStateList.valueOf(ContextCompat.getColor(it, color)) }


        companion object {
            const val layout = R.layout.view_holder_net_control

            fun create(parent: ViewGroup,
                       onTorSwitch: (isChecked: Boolean) -> Unit,
                       onVpnSwitch: (isChecked: Boolean) -> Unit) = TorControlViewHolder(inflate(parent, layout, false), onTorSwitch, onVpnSwitch)
        }

    }
}
