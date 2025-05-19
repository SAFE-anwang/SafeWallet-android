package io.horizontalsystems.bankwallet.modules.safe4

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.providers.Translator.getString
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.balance.BalanceAdapterRepository
import io.horizontalsystems.bankwallet.modules.balance.BalanceCache
import io.horizontalsystems.bankwallet.modules.safe4.linelock.LineLockSendActivity
import io.horizontalsystems.bankwallet.modules.safe4.linelock.LineLockSendFragment
import io.horizontalsystems.bankwallet.modules.safe4.lockinfo.LockInfoActivity
import io.horizontalsystems.bankwallet.modules.safe4.node.NodeType
import io.horizontalsystems.bankwallet.modules.safe4.node.SafeFourModule
import io.horizontalsystems.bankwallet.modules.safe4.node.proposal.SafeFourProposalModule
import io.horizontalsystems.bankwallet.modules.safe4.node.reward.SafeFourRewardModule
import io.horizontalsystems.bankwallet.modules.safe4.node.safe3.RedeemSafe3Module
import io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.WithdrawFragment
import io.horizontalsystems.bankwallet.modules.safe4.node.withdraw.WithdrawModule
import io.horizontalsystems.bankwallet.modules.safe4.safe2wsafe.SafeConvertSendActivity
import io.horizontalsystems.bankwallet.modules.safe4.safe2wsafe.SafeConvertSendFragment
import io.horizontalsystems.bankwallet.modules.safe4.swap.SAFE4SwapFragment
import io.horizontalsystems.bankwallet.modules.safe4.swap.Safe4SwapViewModel
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmModule
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.marketkit.SafeExtend
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenType

object Safe4Module {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return Safe4ViewModel() as T
        }
    }

    fun handlerSafe2eth(chainType: ChainType, navController: NavController) {
        val context = navController.context
        val walletList: List<Wallet> = App.walletManager.activeWallets
        var safeWallet: Wallet? = null
        var wsafeWallet: Wallet? = null
        for (it in walletList) {
//            Log.i("safe4", "---coinType = ${it.token} ---uid = ${it.coin.uid} ---chainType=$chainType")
            if (it.token.blockchain.type is BlockchainType.Safe && it.coin.uid == "safe-coin") {
                safeWallet = it
//                Log.i("safe4", "---safe---")
            } else if (chainType == ChainType.ETH &&  it.token.blockchain.type is BlockchainType.Ethereum && it.coin.uid == "safe-coin") {
                wsafeWallet = it
//                Log.i("safe4", "---erc20---")
            } else if (chainType == ChainType.BSC &&  it.token.blockchain.type is BlockchainType.BinanceSmartChain && (it.coin.uid == "safe-coin")) {
                wsafeWallet = it
//                Log.i("safe4", "---bep20---")
            } else if (chainType == ChainType.MATIC &&  it.token.blockchain.type is BlockchainType.Polygon && (it.coin.uid == "safe-coin")) {
                wsafeWallet = it
            }
        }
        if (safeWallet == null) {
            Toast.makeText(context, getString(R.string.Safe4_Wallet_Tips, "Safe"), Toast.LENGTH_SHORT).show()
            return
        }
        if (wsafeWallet == null) {
            if (chainType == ChainType.ETH) {
                Toast.makeText(context, getString(R.string.Safe4_Wallet_Tips, "Safe ERC20"), Toast.LENGTH_SHORT).show()
            } else if (chainType == ChainType.BSC) {
                Toast.makeText(context, getString(R.string.Safe4_Wallet_Tips, "Safe BEP20"), Toast.LENGTH_SHORT).show()
            } else if (chainType == ChainType.MATIC) {
                Toast.makeText(context, getString(R.string.Safe4_Wallet_Tips, "Safe Matic"), Toast.LENGTH_SHORT).show()
            }
            return
        }
        val balanceAdapterRepository = BalanceAdapterRepository(App.adapterManager, BalanceCache(App.appDatabase.enabledWalletsCacheDao()))
        val state =  balanceAdapterRepository.state(safeWallet)
        if (state is AdapterState.Synced){
            navController.navigate(
                R.id.sendWSafeFragment,
                SafeConvertSendFragment.prepareParams(safeWallet, wsafeWallet, chainType == ChainType.ETH, chainType == ChainType.MATIC)
            )
            /*context.startActivity(Intent(context, SafeConvertSendActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(SafeConvertSendActivity.WALLET_SAFE, safeWallet)
                putExtra(SafeConvertSendActivity.WALLET_WSAFE, wsafeWallet)
                putExtra(SafeConvertSendActivity.IS_ETH, chainType == ChainType.ETH)
                putExtra(SafeConvertSendActivity.IS_MATIC, chainType == ChainType.MATIC)
            })*/
        } else {
            Toast.makeText(context, getString(R.string.Balance_Syncing), Toast.LENGTH_SHORT).show()
        }
    }

    fun handlerEth2safe(chainType: ChainType, navController: NavController) {
        val context = App.instance
        val walletList: List<Wallet> = App.walletManager.activeWallets
        var safeWallet: Wallet? = null
        var wsafeWallet: Wallet? = null
        for (it in walletList) {
//            Log.i("safe4", "---coinType = ${it.coinType} ---uid = ${it.coin.uid} ---chainType = $chainType")
            if (it.token.blockchain.type is BlockchainType.Safe && it.coin.uid == "safe-coin") {
                safeWallet = it
            } else if (chainType == ChainType.ETH && it.token.blockchain.type is BlockchainType.Ethereum && it.coin.uid == "safe-coin") {
                wsafeWallet = it
//                Log.i("safe4", "---erc20---")
            } else if (chainType == ChainType.BSC && it.token.blockchain.type is BlockchainType.BinanceSmartChain && it.coin.uid == "safe-coin") {
                wsafeWallet = it
//                Log.i("safe4", "---bep20---")
            } else if (chainType == ChainType.MATIC &&  it.token.blockchain.type is BlockchainType.Polygon && (it.coin.uid == "safe-coin")) {
                wsafeWallet = it
            }
        }
        if (safeWallet == null) {
            Toast.makeText(context, getString(R.string.Safe4_Wallet_Tips, "Safe"), Toast.LENGTH_SHORT).show()
            return
        }
        if (wsafeWallet == null) {
            if (chainType == ChainType.ETH) {
                Toast.makeText(context, getString(R.string.Safe4_Wallet_Tips, "Safe ERC20"), Toast.LENGTH_SHORT).show()
            } else if (chainType == ChainType.BSC) {
                Toast.makeText(context, getString(R.string.Safe4_Wallet_Tips, "Safe BEP20"), Toast.LENGTH_SHORT).show()
            } else if (chainType == ChainType.MATIC) {
                Toast.makeText(context, getString(R.string.Safe4_Wallet_Tips, "Safe Matic"), Toast.LENGTH_SHORT).show()
            }
            return
        }

        val balanceAdapterRepository = BalanceAdapterRepository(App.adapterManager, BalanceCache(App.appDatabase.enabledWalletsCacheDao()))
        val state =  balanceAdapterRepository.state(wsafeWallet)
        if (state is AdapterState.Synced){
            val bundle = Bundle()
            bundle.putParcelable(SafeConvertSendActivity.WALLET_SAFE, safeWallet)
            bundle.putParcelable(SafeConvertSendActivity.WALLET_WSAFE, wsafeWallet)
            bundle.putBoolean(SafeConvertSendActivity.IS_ETH, chainType == ChainType.ETH)
            bundle.putBoolean(SafeConvertSendActivity.IS_MATIC, chainType == ChainType.MATIC)
            navController.slideFromBottom(
                R.id.sendWsafeFragment,
                SendEvmModule.Input(safeWallet, wsafeWallet, chainType == ChainType.ETH, chainType == ChainType.MATIC)
            )
        } else {
            Toast.makeText(context, getString(R.string.Balance_Syncing), Toast.LENGTH_SHORT).show()
        }
    }


    fun handlerSafe42eth(chainType: ChainType, navController: NavController) {
        val context = navController.context
        val walletList: List<Wallet> = App.walletManager.activeWallets
        var safeWallet: Wallet? = null
        var chain: Chain = Chain.Ethereum
        for (it in walletList) {
            if (it.token.blockchain.type is BlockchainType.SafeFour && it.coin.uid == "safe4-coin" && it.token.type == TokenType.Native) {
                safeWallet = it
            }
        }
        if (safeWallet == null) {
            Toast.makeText(context, getString(R.string.Safe4_Wallet_Tips, "SAFE4"), Toast.LENGTH_SHORT).show()
            return
        }
        if (chainType == ChainType.ETH) {
            chain = Chain.Ethereum
        } else if (chainType == ChainType.BSC) {
            chain = Chain.BinanceSmartChain
        } else if (chainType == ChainType.MATIC) {
            chain = Chain.Polygon
        }
        val balanceAdapterRepository = BalanceAdapterRepository(App.adapterManager, BalanceCache(App.appDatabase.enabledWalletsCacheDao()))
        val state =  balanceAdapterRepository.state(safeWallet)
        if (state is AdapterState.Synced){
            navController.slideFromBottom(
                R.id.sendWSafe4Fragment,
                SendEvmModule.InputSafe4(safeWallet, chain,chainType == ChainType.ETH, chainType == ChainType.MATIC)
            )
        } else {
            Toast.makeText(context, getString(R.string.Balance_Syncing), Toast.LENGTH_SHORT).show()
        }
    }

    fun handlerEth2safe4(chainType: ChainType, navController: NavController) {
        val context = App.instance
        val walletList: List<Wallet> = App.walletManager.activeWallets
        var safeWallet: Wallet? = null
        var wsafeWallet: Wallet? = null
        for (it in walletList) {
//            Log.i("safe4", "---coinType = ${it.coinType} ---uid = ${it.coin.uid} ---chainType = $chainType")
            if (it.token.blockchain.type is BlockchainType.SafeFour && it.coin.uid == "safe4-coin") {
                safeWallet = it
            } else if (chainType == ChainType.ETH && it.token.blockchain.type is BlockchainType.Ethereum
                && it.coin.uid == "safe-coin") {
                wsafeWallet = it
//                Log.i("safe4", "---erc20---")
            } else if (chainType == ChainType.BSC && it.token.blockchain.type is BlockchainType.BinanceSmartChain
                && it.coin.uid ==  "safe-coin") {
                wsafeWallet = it
//                Log.i("safe4", "---bep20---")
            } else if (chainType == ChainType.MATIC &&  it.token.blockchain.type is BlockchainType.Polygon
                && it.coin.uid  == "safe-coin") {
                wsafeWallet = it
            }
        }
        if (safeWallet == null) {
            Toast.makeText(context, getString(R.string.Safe4_Wallet_Tips, "SAFE4"), Toast.LENGTH_SHORT).show()
            return
        }
        if (wsafeWallet == null) {
            if (chainType == ChainType.ETH) {
                Toast.makeText(context, getString(R.string.Safe4_Wallet_Tips, "SAFE ERC20"), Toast.LENGTH_SHORT).show()
            } else if (chainType == ChainType.BSC) {
                Toast.makeText(context, getString(R.string.Safe4_Wallet_Tips, "SAFE BEP20"), Toast.LENGTH_SHORT).show()
            } else if (chainType == ChainType.MATIC) {
                Toast.makeText(context, getString(R.string.Safe4_Wallet_Tips, "SAFE Matic"), Toast.LENGTH_SHORT).show()
            }
            return
        }

        val balanceAdapterRepository = BalanceAdapterRepository(App.adapterManager, BalanceCache(App.appDatabase.enabledWalletsCacheDao()))
        val state =  balanceAdapterRepository.state(wsafeWallet)
        if (state is AdapterState.Synced){
            navController.slideFromBottom(
                R.id.sendWsafeFragment,
                SendEvmModule.Input(safeWallet, wsafeWallet, chainType == ChainType.ETH, chainType == ChainType.MATIC, true)
            )
        } else {
            Toast.makeText(context, getString(R.string.Balance_Syncing), Toast.LENGTH_SHORT).show()
        }
    }


    fun handlerSafe42SRC(navController: NavController) {
        val context = navController.context
        val walletList: List<Wallet> = App.walletManager.activeWallets
        var safeWallet: Wallet? = null
        var safeSRC20Wallet: Wallet? = null
        for (it in walletList) {
            if (it.token.blockchain.type is BlockchainType.SafeFour) {
                if (it.token.type == TokenType.Native) {
                    safeWallet = it
                } else if (it.token.type == TokenType.Eip20(Safe4SwapViewModel.safe4SwapContractAddress)) {
                    safeSRC20Wallet = it
                }
            }
        }
        if (safeWallet == null) {
            Toast.makeText(context, getString(R.string.Safe4_Wallet_Tips, "SAFE"), Toast.LENGTH_SHORT).show()
            return
        }
        if (safeSRC20Wallet == null) {
            Toast.makeText(context, getString(R.string.Safe4_Wallet_Tips, "SAFE SRC20"), Toast.LENGTH_SHORT).show()
            return
        }
        val balanceAdapterRepository = BalanceAdapterRepository(App.adapterManager, BalanceCache(App.appDatabase.enabledWalletsCacheDao()))
        val state =  balanceAdapterRepository.state(safeWallet)
        val state2 =  balanceAdapterRepository.state(safeSRC20Wallet)
        if (state is AdapterState.Synced && state2 is AdapterState.Synced){
            navController.slideFromBottom(
                R.id.safe4SwapFragment,
                SAFE4SwapFragment.Input(safeWallet, safeSRC20Wallet)
            )
        } else {
            Toast.makeText(context, getString(R.string.Balance_Syncing), Toast.LENGTH_SHORT).show()
        }
    }



    fun handlerWithdraw(withdrawType: WithdrawType, navController: NavController) {
        val context = navController.context
        val walletList: List<Wallet> = App.walletManager.activeWallets
        var safeWallet: Wallet? = null
        for (it in walletList) {
            if (it.token.blockchain.type is BlockchainType.SafeFour && it.coin.uid == "safe4-coin" && it.token.type == TokenType.Native) {
                safeWallet = it
            }
        }
        if (safeWallet == null) {
            Toast.makeText(context, getString(R.string.Safe4_Wallet_Tips, "SAFE4"), Toast.LENGTH_SHORT).show()
            return
        }

        val balanceAdapterRepository = BalanceAdapterRepository(App.adapterManager, BalanceCache(App.appDatabase.enabledWalletsCacheDao()))
        val state =  balanceAdapterRepository.state(safeWallet)
        if (state is AdapterState.Synced){
            when(withdrawType) {
                WithdrawType.SuperNode,
                WithdrawType.MasterNode -> {
                    navController.slideFromBottom(
                        R.id.safe4WithdrawNodeFragment,
                        WithdrawModule.Input(withdrawType == WithdrawType.SuperNode, safeWallet)
                    )
                }
                WithdrawType.Proposal -> {
                    navController.slideFromBottom(
                        R.id.safe4WithdrawProposalFragment,
                        WithdrawModule.Input(false, safeWallet)
                    )
                }
                WithdrawType.Profit -> {
                    navController.slideFromBottom(
                        R.id.rewardFragment,
                        SafeFourRewardModule.Input(safeWallet)
                    )
                }
                WithdrawType.Vote -> {
                    navController.slideFromBottom(
                        R.id.safe4WithdrawVoteFragment,
                        WithdrawModule.Input(false, safeWallet)
                    )
                }
            }
        } else {
            Toast.makeText(context, getString(R.string.Balance_Syncing), Toast.LENGTH_SHORT).show()
        }
    }


    fun handlerLineLock(navController: NavController) {
        val context = navController.context
        val walletList: List<Wallet> = App.walletManager.activeWallets
        var safeWallet: Wallet? = null
        for (it in walletList) {
            if (it.token.blockchain.type is BlockchainType.Safe && it.coin.uid == "safe-coin") {
                safeWallet = it
            }
        }
        if (safeWallet == null) {
            Toast.makeText(context, getString(R.string.Safe4_Wallet_Tips, "Safe"), Toast.LENGTH_SHORT).show()
            return
        }
        val balanceAdapterRepository = BalanceAdapterRepository(App.adapterManager, BalanceCache(App.appDatabase.enabledWalletsCacheDao()))
        val state =  balanceAdapterRepository.state(safeWallet)
        if (state is AdapterState.Synced){
            val bundle = Bundle()
            bundle.putParcelable(LineLockSendActivity.WALLET, safeWallet)
            navController.navigate(
                R.id.sendSafeLockFragment,
                bundle
            )
            /*context.startActivity(Intent(context, LineLockSendActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(LineLockSendActivity.WALLET, safeWallet)
            })*/
        } else {
            Toast.makeText(context, getString(R.string.Balance_Syncing), Toast.LENGTH_SHORT).show()
        }
    }

    fun handlerLineInfo() {
        val context = App.instance
        val walletList: List<Wallet> = App.walletManager.activeWallets
        var safeWallet: Wallet? = null
        for (it in walletList) {
            if (it.token.blockchain.type is BlockchainType.Safe && it.coin.uid == "safe-coin") {
                safeWallet = it
            }
        }
        if (safeWallet == null) {
            Toast.makeText(context, getString(R.string.Safe4_Wallet_Tips, "Safe"), Toast.LENGTH_SHORT).show()
            return
        }
        val balanceAdapterRepository = BalanceAdapterRepository(App.adapterManager, BalanceCache(App.appDatabase.enabledWalletsCacheDao()))
        val state =  balanceAdapterRepository.state(safeWallet)
        if (state is AdapterState.Synced){
            context.startActivity(Intent(context, LockInfoActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(LockInfoActivity.WALLET, safeWallet)
            })
        } else {
            Toast.makeText(context, getString(R.string.Balance_Syncing), Toast.LENGTH_SHORT).show()
        }
    }

    fun handlerNode(type: SafeFourType, navController: NavController) {
        if (App.accountManager.activeAccount?.type is AccountType.EvmAddress) return
        val context = App.instance
        val walletList: List<Wallet> = App.walletManager.activeWallets
        var safe4Wallet: Wallet? = null
        for (it in walletList) {
            if (it.token.blockchain.type is BlockchainType.SafeFour && it.token.type == TokenType.Native) {
                safe4Wallet = it
            }
        }
        if (safe4Wallet == null) {
            Toast.makeText(context, getString(R.string.Safe4_Wallet_Tips, "SAFE4"), Toast.LENGTH_SHORT).show()
            return
        }
        val balanceAdapterRepository = BalanceAdapterRepository(App.adapterManager, BalanceCache(App.appDatabase.enabledWalletsCacheDao()))
        val state =  balanceAdapterRepository.state(safe4Wallet)
        if (state is AdapterState.Synced){
            when(type) {
                SafeFourType.SuperNode -> {
                    navController.slideFromBottom(
                            R.id.nodeListFragment,
                            SafeFourModule.Input(R.string.Safe_Four_Super_Node, NodeType.SuperNode.ordinal, safe4Wallet)
                    )
                }
                SafeFourType.MasterNode -> {
                    navController.slideFromBottom(
                            R.id.nodeListFragment,
                            SafeFourModule.Input(R.string.Safe_Four_Master_Node, NodeType.MainNode.ordinal, safe4Wallet)
                    )
                }
                SafeFourType.Proposal -> {
                    val adapter = (App.adapterManager.getAdapterForWallet(safe4Wallet) as? ISendEthereumAdapter)
                    if (adapter!!.evmKitWrapper.evmKit.lastBlockHeight!! <= 86400) {
                        Toast.makeText(context, getString(R.string.Proposal_NOT_CREATE), Toast.LENGTH_SHORT).show()
                    } else {
                        navController.slideFromBottom(
                            R.id.proposalFragment,
                            SafeFourProposalModule.Input(safe4Wallet)
                        )
                    }
                }
                SafeFourType.Redeem -> {
                    var safeWallet: Wallet? = null
                    for (it in walletList) {
                        if (it.token.blockchain.type is BlockchainType.Safe && it.coin.uid == "safe-coin") {
                            safeWallet = it
                        }
                    }
                    navController.slideFromBottom(
                            R.id.redeemSafe3SelectFragment,
                            RedeemSafe3Module.Input(safe4Wallet, safeWallet)
                    )
                }
                SafeFourType.Locked -> {
                    navController.slideFromBottom(
                            R.id.safe4LockedInfoFragment,
                            WithdrawModule.Input(false, safe4Wallet)
                    )
                }
            }
        } else {
            Toast.makeText(context, getString(R.string.Balance_Syncing), Toast.LENGTH_SHORT).show()
        }
    }

    fun getSafe3TestCoin(navController: NavController) {
        if (App.accountManager.activeAccount?.type is AccountType.EvmAddress) return
        val context = App.instance
        val walletList: List<Wallet> = App.walletManager.activeWallets
        var safeWallet: Wallet? = null
        for (it in walletList) {
            if (it.token.blockchain.type is BlockchainType.SafeFour && it.token.type == TokenType.Native) {
                safeWallet = it
            }
        }
        navController.slideFromBottom(
                R.id.getSafe3TestCoinFragment,
                RedeemSafe3Module.GetTestCoinInput(safeWallet)
        )

    }

    enum class SafeFourType {
        SuperNode, MasterNode, Proposal, Redeem, Locked
    }

    enum class ChainType {
        ETH, BSC, MATIC
    }

    enum class WithdrawType {
        SuperNode, MasterNode, Proposal, Profit, Vote
    }
}
