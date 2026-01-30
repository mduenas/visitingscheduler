import Foundation
import GoogleMobileAds
import UIKit

/// Manager for Google AdMob integration on iOS.
/// Handles banner ad display and configuration.
class AdMobManager: NSObject, ObservableObject {

    /// Banner ad unit IDs
    static let bannerAdUnitId = "ca-app-pub-7540731406850248/3826080117"
    static let testBannerAdUnitId = "ca-app-pub-3940256099942544/2934735716"

    /// Singleton instance
    static let shared = AdMobManager()

    /// Published state
    @Published private(set) var isInitialized: Bool = false

    /// Whether to use test ads (set based on build configuration)
    #if DEBUG
    let useTestAds = true
    #else
    let useTestAds = false
    #endif

    private override init() {
        super.init()
    }

    /// Initialize the Mobile Ads SDK
    func initialize() {
        GADMobileAds.sharedInstance().start { [weak self] status in
            print("AdMob SDK initialized")
            self?.isInitialized = true

            // Log adapter statuses for debugging
            let adapterStatuses = status.adapterStatusesByClassName
            for (adapter, status) in adapterStatuses {
                print("Adapter: \(adapter), State: \(status.state.rawValue), Description: \(status.description)")
            }
        }
    }

    /// Get the appropriate banner ad unit ID
    var bannerAdUnitId: String {
        useTestAds ? Self.testBannerAdUnitId : Self.bannerAdUnitId
    }

    /// Create a banner ad view
    func createBannerView(rootViewController: UIViewController) -> GADBannerView {
        let bannerView = GADBannerView(adSize: GADAdSizeBanner)
        bannerView.adUnitID = bannerAdUnitId
        bannerView.rootViewController = rootViewController
        bannerView.delegate = self
        return bannerView
    }

    /// Load an ad into a banner view
    func loadAd(bannerView: GADBannerView) {
        let request = GADRequest()
        bannerView.load(request)
    }
}

// MARK: - GADBannerViewDelegate

extension AdMobManager: GADBannerViewDelegate {

    func bannerViewDidReceiveAd(_ bannerView: GADBannerView) {
        print("Banner ad loaded successfully")
    }

    func bannerView(_ bannerView: GADBannerView, didFailToReceiveAdWithError error: Error) {
        print("Banner ad failed to load: \(error.localizedDescription)")
    }

    func bannerViewWillPresentScreen(_ bannerView: GADBannerView) {
        print("Banner ad will present screen")
    }

    func bannerViewWillDismissScreen(_ bannerView: GADBannerView) {
        print("Banner ad will dismiss screen")
    }

    func bannerViewDidDismissScreen(_ bannerView: GADBannerView) {
        print("Banner ad did dismiss screen")
    }
}
