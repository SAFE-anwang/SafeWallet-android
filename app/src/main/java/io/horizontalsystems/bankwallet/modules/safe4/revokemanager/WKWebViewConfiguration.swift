import Foundation
import JavaScriptCore
import WebKit
import EvmKit

enum Web3Method {
    case transactionHandler
    case ethRequestAccounts
    case ethAccounts
    case ethChainId
    case personalSign
    case ethSendTransaction
    case walletSwitchChain
    case walletAddChain
    case unsupported(method: String)
    
    init(method: String) {
        switch method {
        case "transactionHandler": self = .transactionHandler
        case "eth_requestAccounts": self = .ethRequestAccounts
        case "eth_accounts": self = .ethAccounts
        case "eth_chainId": self = .ethChainId
        case "personal_sign": self = .personalSign
        case "eth_sendTransaction": self = .ethSendTransaction
        case "wallet_switchEthereumChain": self = .walletSwitchChain
        case "wallet_addEthereumChain": self = .walletAddChain
        default: self = .unsupported(method: method)
        }
    }
    
    var name: String {
        switch self {
        case .transactionHandler: return "transactionHandler"
        case .ethRequestAccounts: return "eth_requestAccounts"
        case .ethAccounts: return "eth_accounts"
        case .ethChainId: return "eth_chainId"
        case .personalSign: return "personal_sign"
        case .ethSendTransaction: return "eth_sendTransaction"
        case .walletSwitchChain: return "wallet_switchEthereumChain"
        case .walletAddChain: return "wallet_addEthereumChain"
        case .unsupported(let method): return method
        }
    }
}

extension Web3Method: Hashable {
    public func hash(into hasher: inout Hasher) {
        hasher.combine(name)
    }
}

extension Web3Method: Equatable {
    public static func == (lhs: Web3Method, rhs: Web3Method) -> Bool {
        lhs.name == rhs.name
    }
}

extension WKWebViewConfiguration {
    
    static func make(forChainId chainId: Int, address: String, messageHandler: WKScriptMessageHandler) -> WKWebViewConfiguration {
        let webViewConfig = WKWebViewConfiguration()

        let js = """
        class CustomEthereumProvider {
            constructor() {
                this.chainId = "0x\(String(chainId, radix: 16))";
                this.selectedAddress = "\(address)";
                this.isConnected = true;
                this._listeners = {};
                this._nextId = 1;
                this._pendingRequests = {};
            }
            request(request) {
                return new Promise((resolve, reject) => {
                    const id = this._nextId++;
                    this._pendingRequests[id] = { resolve, reject };
                    
                    if (request.method === '\(Web3Method.ethSendTransaction.name)') {
                        
                        window.webkit.messageHandlers.transactionHandler.postMessage({
                            type: 'transaction',
                            id: id,
                            method: request.method,
                            params: request.params
                        });
                        return;
                    }
                    
                    switch(request.method) {
                        case 'eth_requestAccounts':
                            resolve([this.selectedAddress]);
                            break;
                        case 'eth_accounts':
                            resolve([this.selectedAddress]);
                            break;
                        case 'eth_chainId':
                            resolve(this.chainId);
                            break;
                        case 'wallet_addEthereumChain':
                            resolve();
                            break;
                        case 'wallet_switchEthereumChain':
                            this.chainId = request.params[0].chainId;
                            this.emit('chainChanged', this.chainId);
                            resolve();
                            break;
                        default:
                            console.log('[CustomProvider] Unhandled request:', request);
                            reject(new Error('Method not implemented'));
                    }
        
                    if (request.method === '\(Web3Method.walletSwitchChain.name)') {
                        window.webkit.messageHandlers.transactionHandler.postMessage({
                            type: 'transaction',
                            id: id,
                            method: request.method,
                            params: request.params
                        });
                        return;
                    }
                });
            } 
            // 实现事件系统
            on(event, listener) {
                if (!this._listeners[event]) this._listeners[event] = [];
                this._listeners[event].push(listener);
            }
            removeListener(event, listener) {
                const idx = this._listeners[event]?.indexOf(listener);
                if (idx >= 0) this._listeners[event].splice(idx, 1);
            }
            emit(event, ...args) {
                this._listeners[event]?.forEach(fn => fn(...args));
            }
            
            handleResponse(id, result, error) {
                const request = this._pendingRequests[id];
                if (request) {
                    if (error) {
                        request.reject(new Error(error));
                    } else {
                        request.resolve(result);
                    }
                    delete this._pendingRequests[id];
                }
            }
        }

        window.ethereum = new CustomEthereumProvider(
            "\(address)",
            "0x\(String(chainId, radix: 16))"
        );
        
        // 触发连接事件
        setTimeout(() => {
            window.dispatchEvent(new Event('ethereum#initialized'));
            window.ethereum.emit('connect', { 
                chainId: window.ethereum.chainId 
            });
            
            console.log('[CustomProvider] Wallet connected:', 
                window.ethereum.selectedAddress, 
                'on chain', 
                window.ethereum.chainId
            );
        }, 1000);
        
        window.handleProviderResponse = function(id, result, error) {
            window.ethereum.handleResponse(id, result, error);
        };
        """
        
        let userScript = WKUserScript(source: js, injectionTime: .atDocumentStart, forMainFrameOnly: false)
        webViewConfig.userContentController.addUserScript(userScript)
        webViewConfig.userContentController.add(messageHandler, name: Web3Method.transactionHandler.name)
        webViewConfig.userContentController.add(messageHandler, name: Web3Method.walletSwitchChain.name)
        webViewConfig.userContentController.add(messageHandler, name: Web3Method.ethSendTransaction.name)
        webViewConfig.userContentController.add(messageHandler, name: Web3Method.ethChainId.name)

        return webViewConfig
    }
}

