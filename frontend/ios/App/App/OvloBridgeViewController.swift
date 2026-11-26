import Capacitor
import UIKit

/// 앱 안에 둔 로컬 플러그인을 브리지에 등록한다(npm 패키지 플러그인은 cap sync 가 등록한다).
class OvloBridgeViewController: CAPBridgeViewController {
    override open func capacitorDidLoad() {
        bridge?.registerPluginInstance(AppleSignInPlugin())
    }
}
