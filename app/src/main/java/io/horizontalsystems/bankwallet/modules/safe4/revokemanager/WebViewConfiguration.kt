package io.horizontalsystems.bankwallet.modules.safe4.revokemanager

import android.util.Log
import android.webkit.WebView
import io.horizontalsystems.ethereumkit.core.toHexString

object WebViewConfiguration {


    fun make(chainId: Int, address: String, webView: WebView) {


        val js = """
        class CustomEthereumProvider {
            constructor() {
                this.chainId = "${(chainId.toHexString())}";
                this.selectedAddress = "${address}";
                this.isConnected = true;
                this._listeners = {};
                this._nextId = 1;
                this._pendingRequests = {};
            }
            request(request) {
                return new Promise((resolve, reject) => {
                    const id = this._nextId++;
                    this._pendingRequests[id] = { resolve, reject };
                    
                    if (request.method === '${Web3Method.ethSendTransaction.name}') {
                        
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
        
                    if (request.method === '${Web3Method.walletSwitchChain.name}') {
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
            "${address}",
            "${chainId.toHexString()}"
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
        webView.evaluateJavascript(js, {
            Log.d("longwen", "js result=$it")
        })
//        let userScript = WKUserScript(source: js, injectionTime: .atDocumentStart, forMainFrameOnly: false)
//        webViewConfig.userContentController.addUserScript(userScript)
//        webViewConfig.userContentController.add(messageHandler, name: Web3Method.transactionHandler.name)
//        webViewConfig.userContentController.add(messageHandler, name: Web3Method.walletSwitchChain.name)
//        webViewConfig.userContentController.add(messageHandler, name: Web3Method.ethSendTransaction.name)
//        webViewConfig.userContentController.add(messageHandler, name: Web3Method.ethChainId.name)

//        return webViewConfig
    }
}



enum class Web3Method(val methodName: String) {
    transactionHandler("transactionHandler"),
    ethRequestAccounts("eth_requestAccounts"),
    ethAccounts("eth_accounts"),
    ethChainId("eth_chainId"),
    personalSign("personal_sign"),
    ethSendTransaction("eth_sendTransaction"),
    walletSwitchChain("wallet_switchEthereumChain"),
    walletAddChain("wallet_addEthereumChain"),
    unsupported("Unsupported");

    fun getMethod(method: String): Web3Method {
        return when (method) {
            "transactionHandler" -> transactionHandler
            "eth_requestAccounts" -> ethRequestAccounts
            "eth_accounts" -> ethAccounts
            "eth_chainId" -> ethChainId
            "personal_sign" -> personalSign
            "eth_sendTransaction" -> ethSendTransaction
            "wallet_switchEthereumChain" -> walletSwitchChain
            "wallet_addEthereumChain" -> walletAddChain
            else -> unsupported
        }
    }

    fun getMethodName(method: Web3Method): String {
        return when(method) {
            transactionHandler -> "transactionHandler"
            ethRequestAccounts -> "eth_requestAccounts"
            ethAccounts -> "eth_accounts"
            ethChainId -> "eth_chainId"
            personalSign -> "personal_sign"
            ethSendTransaction -> "eth_sendTransaction"
            walletSwitchChain -> "wallet_switchEthereumChain"
            walletAddChain -> "wallet_addEthereumChain"
            unsupported -> method.name
        }
    }
}