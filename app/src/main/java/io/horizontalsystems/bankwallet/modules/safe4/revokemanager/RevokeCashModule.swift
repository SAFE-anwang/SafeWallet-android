import Foundation
import MarketKit
import UIKit

struct RevokeCashModule {
    
    static func viewController(blockchainType: BlockchainType) -> UIViewController? {
        let chain = App.shared.evmBlockchainManager.chain(blockchainType: blockchainType)
        guard let account = App.shared.accountManager.activeAccount else { return nil }
        do {
            let evmKitWrapper = try App.shared.evmBlockchainManager.evmKitManager(blockchainType: blockchainType).evmKitWrapper(account: account, blockchainType: blockchainType)
            let address = try App.shared.evmBlockchainManager.evmKitManager(blockchainType: blockchainType).evmKitWrapper(account: account, blockchainType: blockchainType).evmKit.address
            let viewModel = RevokeCashViewModel(walletAddress: address, chain: chain, account: account)
            let viewController = RevokeCashView(viewModel: viewModel).toViewController()
            viewController.hidesBottomBarWhenPushed = true
            return viewController
        }catch {
            return nil
        }
    }
}
