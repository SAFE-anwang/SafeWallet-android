import WebKit
import HsToolKit
import EvmKit
import Combine


enum MessageHandlerType {
    case sendRevokeTransaction(transactionData: TransactionData)
    case switchEthereumChain(chainIdHex: String)
    case unknow
}

class RevokeCashMessageHandler: NSObject, WKScriptMessageHandler {
    
    @Published var messageHandler: MessageHandlerType = .unknow
    
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        guard let body = message.body as? [String: Any] else { return }
        
        if let type = body["type"] as? String {
            switch type {
            case "transaction":
                handleRPCRequest(body)
            default: break
            }
        }
    }
    private func handleRPCRequest(webView: WKWebView) {
        
    }
    
    private func handleRPCRequest(_ payload: [String: Any]?) {
        guard let methodName = payload?["method"] as? String,
              let id = payload?["id"] as? Int,
              let params = payload?["params"] as? [Any] else {
            return
        }
        let method = Web3Method(method: methodName)
        switch method {
        case .ethSendTransaction:
            processRevokeTransaction(id: id, params: params)
            
        case .walletSwitchChain:
            processSwitchEthereumChain(id: id, params: params)
            
        default:
            print("未处理的 RPC 方法: \(method)")
        }
    }
    
    private func processSwitchEthereumChain(id: Int, params: [Any]) {
        guard let txObject = params.first as? [String: Any],
              let chainIdHex = txObject["chainId"] as? String else {
            return
        }
        messageHandler = .switchEthereumChain(chainIdHex: chainIdHex)
    }
    
    private func processRevokeTransaction(id: Int, params: [Any]) {
        guard let txObject = params.first as? [String: Any],
              let fromAddress = txObject["from"] as? String,
              let toAddress = txObject["to"] as? String,
              let data = txObject["data"] as? String else {
            return
        }
        guard let input = data.hs.hexData else { return }

        do {
            let to = try EvmKit.Address(hex: toAddress)
            let transactionData = TransactionData(to: to, value: 0, input: input)
            messageHandler = .sendRevokeTransaction(transactionData: transactionData)
        }catch{
            print("error")
        }

    }
            

}
/*
struct RevokeSpenderAddresses {
    let erc20: [String]
    let erc721: [String]
}

class AddressExtractor {

    static func extractSpenderAddresses(from data: String) -> RevokeSpenderAddresses {
        let cleanedData = data.hasPrefix("0x") ? String(data.dropFirst(2)) : data
        
        let approveSelector = "095ea7b3" // ERC20 approve
        let setApprovalSelector = "a22cb465" // ERC721/ERC1155 setApprovalForAll
        
        var erc20Addresses: [String] = []
        var erc721Addresses: [String] = []
        var searchStartIndex = cleanedData.startIndex
        
        while let range = cleanedData.range(of: approveSelector, range: searchStartIndex..<cleanedData.endIndex) {
            let paramsStart = range.upperBound
            if cleanedData.distance(from: paramsStart, to: cleanedData.endIndex) >= 64 {
                let spenderHex = String(cleanedData[paramsStart..<cleanedData.index(paramsStart, offsetBy: 64)])
                erc20Addresses.append("0x" + spenderHex.suffix(40))
                searchStartIndex = cleanedData.index(paramsStart, offsetBy: 64)
            } else {
                break
            }
        }
        
        searchStartIndex = cleanedData.startIndex
        
        while let range = cleanedData.range(of: setApprovalSelector, range: searchStartIndex..<cleanedData.endIndex) {
            let paramsStart = range.upperBound
            if cleanedData.distance(from: paramsStart, to: cleanedData.endIndex) >= 64 {
                let spenderHex = String(cleanedData[paramsStart..<cleanedData.index(paramsStart, offsetBy: 64)])
                erc721Addresses.append("0x" + spenderHex.suffix(40))
                searchStartIndex = cleanedData.index(paramsStart, offsetBy: 64)
            } else {
                break
            }
        }
        return RevokeSpenderAddresses(erc20: Array(Set(erc20Addresses)), erc721: Array(Set(erc721Addresses)))

    }
}
*/
