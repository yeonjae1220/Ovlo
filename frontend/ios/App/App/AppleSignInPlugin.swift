import AuthenticationServices
import Capacitor
import UIKit

/// Sign in with Apple 네이티브 시트.
///
/// 커뮤니티 플러그인(@capacitor-community/apple-sign-in 7.x)은 capacitor-swift-pm 7.x 에 묶여 있어
/// Capacitor 8 앱의 SPM 해석이 실패한다. 필요한 건 authorize 하나뿐이라 앱 안에 둔다.
/// JS: `registerPlugin('AppleSignIn').authorize({ nonce })`
///   → `{ identityToken, authorizationCode, givenName, familyName, email }`.
/// authorizationCode 는 탈퇴 때 서버가 Apple 토큰으로 바꿔 폐기하는 데 쓴다(5분·1회용).
/// nonce 는 원본의 SHA-256 hex 를 받는다(토큰의 nonce 클레임에 그대로 들어간다).
@objc(AppleSignInPlugin)
public class AppleSignInPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "AppleSignInPlugin"
    public let jsName = "AppleSignIn"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "authorize", returnType: CAPPluginReturnPromise),
    ]

    private var pendingCall: CAPPluginCall?
    private var controller: ASAuthorizationController?

    @objc func authorize(_ call: CAPPluginCall) {
        guard let nonce = call.getString("nonce"), !nonce.isEmpty else {
            call.reject("nonce is required", "INVALID_ARGUMENT")
            return
        }
        DispatchQueue.main.async {
            // 이전 요청은 취소로 끝낸다. 그 컨트롤러의 콜백이 늦게 와도 아래 delegate 가 무시한다.
            if let previous = self.pendingCall {
                previous.reject("superseded by a new request", "CANCELED")
            }
            let request = ASAuthorizationAppleIDProvider().createRequest()
            request.requestedScopes = [.fullName, .email]
            request.nonce = nonce
            request.state = call.getString("state")

            let controller = ASAuthorizationController(authorizationRequests: [request])
            controller.delegate = self
            controller.presentationContextProvider = self
            self.pendingCall = call
            self.controller = controller
            controller.performRequests()
        }
    }

    private func finish() {
        pendingCall = nil
        controller = nil
    }
}

extension AppleSignInPlugin: ASAuthorizationControllerDelegate {
    public func authorizationController(controller: ASAuthorizationController,
                                        didCompleteWithAuthorization authorization: ASAuthorization) {
        guard controller === self.controller, let call = pendingCall else { return }
        defer { finish() }
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
              let tokenData = credential.identityToken,
              let identityToken = String(data: tokenData, encoding: .utf8),
              let codeData = credential.authorizationCode,
              let authorizationCode = String(data: codeData, encoding: .utf8) else {
            call.reject("Apple did not return an identity token", "NO_TOKEN")
            return
        }
        call.resolve([
            "identityToken": identityToken,
            "authorizationCode": authorizationCode,
            "givenName": credential.fullName?.givenName ?? NSNull(),
            "familyName": credential.fullName?.familyName ?? NSNull(),
            "email": credential.email ?? NSNull(),
        ])
    }

    public func authorizationController(controller: ASAuthorizationController,
                                        didCompleteWithError error: Error) {
        guard controller === self.controller, let call = pendingCall else { return }
        defer { finish() }
        if let authError = error as? ASAuthorizationError, authError.code == .canceled {
            call.reject("canceled", "CANCELED")
        } else {
            call.reject(error.localizedDescription, "FAILED", error)
        }
    }
}

extension AppleSignInPlugin: ASAuthorizationControllerPresentationContextProviding {
    public func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        bridge?.viewController?.view.window ?? ASPresentationAnchor()
    }
}
