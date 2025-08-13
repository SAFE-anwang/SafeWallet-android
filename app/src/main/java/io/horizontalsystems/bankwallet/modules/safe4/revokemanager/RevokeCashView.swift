import SwiftUI
@preconcurrency import WebKit

struct RevokeCashView: View {

    @StateObject private var viewModel: RevokeCashViewModel
    @State var isReload: Bool = false
    
    init(viewModel: RevokeCashViewModel) {
        _viewModel = StateObject(wrappedValue: viewModel)
    }
    var body: some View {
        ThemeView {
            Web3WebView(viewModel: viewModel, isReload: $isReload)
        }
        .navigationTitle("授权管理")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(item: $viewModel.presentDestination, onDismiss: {
            
        }) { present in
            switch present {
            case let .toConfirmation(vc):
                SendEvmConfirmationView(viewController: vc)
            }
        }
        .onAppear() {
            viewModel.make()
        }
    }
}

struct Web3WebView: UIViewRepresentable {

    let viewModel: RevokeCashViewModel
    @Binding var isReload: Bool
    
    func makeUIView(context: Context) -> WKWebView {
        let webView = WKWebView(frame: .zero, configuration: viewModel.config)
        webView.navigationDelegate = context.coordinator
        return webView
    }
    
    func updateUIView(_ webView: WKWebView, context: Context) {
        let request = URLRequest(url: viewModel.dappUrl)
        webView.load(request)
    }
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject, WKNavigationDelegate {
        var parent: Web3WebView
        private var hasInjected = false
        
        init(_ parent: Web3WebView) {
            self.parent = parent
        }
        
        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            
        }

    }
}




