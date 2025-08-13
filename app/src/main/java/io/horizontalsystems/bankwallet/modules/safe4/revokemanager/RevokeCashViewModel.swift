import Foundation
import Combine
import WalletConnectSign
import JavaScriptCore
import UIKit
import WebKit
import EvmKit
import SwiftUI

class RevokeCashViewModel:NSObject, ObservableObject {
    private(set) var chain: Chain
    private(set) var walletAddress: EvmKit.Address
    private(set) var account: Account
    @Published var presentDestination: PresentDestination? = nil
    @State private(set) var chainId: Int
    private var cancellables = Set<AnyCancellable>()
    let messageHandler = RevokeCashMessageHandler()
    private var revokeConnectManager = RevokeConnectManager()
    
    init(walletAddress: EvmKit.Address, chain: Chain, account: Account) {
        self.walletAddress = walletAddress
        self.account = account
        self.chain = chain
        self.chainId = chain.id
        
        let newInfo = RevokeConnectInfo(walletAddress: walletAddress.eip55, chainId: chain.id, selectedAccountId: account.id, isConnected: false)
        revokeConnectManager.save(to: newInfo)
    }
    
    func make() {
        messageHandler.$messageHandler
            .receive(on: DispatchQueue.main)
            .sink { [weak self] message in
                
                switch message {
                case let .sendRevokeTransaction(transactionData):
                    self?.sendTransaction(transactionData: transactionData)
                    
                case let .switchEthereumChain(chainIdHex):
                    self?.chainId = Int(chainIdHex, radix: 16) ?? 0
                    
                case .unknow: ()
                }
            }
            .store(in: &cancellables)
    }

    var config: WKWebViewConfiguration {
        let config = WKWebViewConfiguration.make(forChainId: chainId, address: walletAddress.eip55, messageHandler: messageHandler)
        config.websiteDataStore = WKWebsiteDataStore.default()
        return config
    }

    var dappUrl: URL {
        if let info = RevokeConnectManager().info {
            return URL(string: "https://revoke.cash/zh/address/\(info.walletAddress)?chainId=\(info.chainId)")!
        }else {
            return URL(string: "https://revoke.cash")!
        }
    }
    
    private func sendTransaction(transactionData: TransactionData) {
        guard let blockchain = App.shared.evmBlockchainManager.blockchain(chainId: chainId) else { return }
        do {
            let info = SendEvmData.DAppInfo(name: "RevokeCash", chainName: nil, address: nil)
            let sendData = SendEvmData(transactionData: transactionData, additionalInfo: .otherDApp(info: info), warnings: [])
            let evmKitWrapper = try App.shared.evmBlockchainManager.evmKitManager(blockchainType: blockchain.type).evmKitWrapper(account: account, blockchainType: blockchain.type)
            if let vc = SendEvmConfirmationModule.viewController(evmKitWrapper: evmKitWrapper, sendData: sendData) {
                DispatchQueue.main.async { [self] in
                    presentDestination = .toConfirmation(vc: vc)
                }
            }
        }catch{}
    }
}


class RevokeConnectManager: ObservableObject {
    
    let saveInfoKey = "revokeConnectInfo"
    
    @Published var info: RevokeConnectInfo? {
        didSet { saveToUserDefaults() }
    }
    
    init() {
        self.info = UserDefaults.standard.load(forKey: saveInfoKey)
    }
    
    func save(to newInfo: RevokeConnectInfo) {
        guard newInfo != info else {return }
        info = newInfo
    }
    private func saveToUserDefaults() {
        UserDefaults.standard.save(info, forKey: saveInfoKey)
    }
}

struct RevokeConnectInfo: Codable, Equatable {
    var walletAddress: String
    var chainId: Int
    var selectedAccountId: String
    private(set) var isConnected = false

    mutating func connect() {
        isConnected = true
    }
    
    mutating func disconnect() {
        isConnected = false
    }
    
    public static func == (lhs: RevokeConnectInfo, rhs: RevokeConnectInfo) -> Bool {
        lhs.walletAddress == rhs.walletAddress &&
        lhs.chainId == rhs.chainId &&
        lhs.selectedAccountId == rhs.selectedAccountId
    }
}

extension UserDefaults {
    func save<T: Codable>(_ value: T, forKey key: String) {
        do {
                let data = try JSONEncoder().encode(value)
                UserDefaults.standard.set(data, forKey: key)
            } catch {
                print("Save Encode Error: \(error)")
            }
    }
    
    func load<T: Codable>(forKey key: String) -> T? {
        guard let data = UserDefaults.standard.data(forKey: key) else { return nil }
            do {
                return try JSONDecoder().decode(T.self, from: data)
            } catch {
                print("Load Error: \(error)")
                return nil
            }
    }
}

enum PresentDestination: Hashable, Identifiable {
    case toConfirmation(vc: UIViewController)
    var id: Self {
        self
    }
}
