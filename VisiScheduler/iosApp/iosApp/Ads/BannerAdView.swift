import SwiftUI
import GoogleMobileAds
import UIKit

/// SwiftUI wrapper for Google AdMob banner ads.
/// Automatically hides when user has purchased ad removal.
struct BannerAdView: UIViewRepresentable {

    @ObservedObject var storeKitManager = StoreKitManager.shared
    @ObservedObject var adMobManager = AdMobManager.shared

    func makeUIView(context: Context) -> UIView {
        let containerView = UIView()
        containerView.backgroundColor = .clear

        // Don't show ads if user has purchased removal
        guard !storeKitManager.hasRemovedAds else {
            return containerView
        }

        // Create and configure banner view
        let bannerView = GADBannerView(adSize: GADAdSizeBanner)
        bannerView.adUnitID = adMobManager.bannerAdUnitId
        bannerView.delegate = context.coordinator
        bannerView.translatesAutoresizingMaskIntoConstraints = false

        containerView.addSubview(bannerView)

        NSLayoutConstraint.activate([
            bannerView.centerXAnchor.constraint(equalTo: containerView.centerXAnchor),
            bannerView.centerYAnchor.constraint(equalTo: containerView.centerYAnchor)
        ])

        // Store reference for later use
        context.coordinator.bannerView = bannerView

        return containerView
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        // Hide/show based on purchase state
        if storeKitManager.hasRemovedAds {
            context.coordinator.bannerView?.removeFromSuperview()
            context.coordinator.bannerView = nil
        } else if context.coordinator.bannerView == nil {
            // Recreate banner if needed
            let bannerView = GADBannerView(adSize: GADAdSizeBanner)
            bannerView.adUnitID = adMobManager.bannerAdUnitId
            bannerView.delegate = context.coordinator
            bannerView.translatesAutoresizingMaskIntoConstraints = false

            uiView.addSubview(bannerView)

            NSLayoutConstraint.activate([
                bannerView.centerXAnchor.constraint(equalTo: uiView.centerXAnchor),
                bannerView.centerYAnchor.constraint(equalTo: uiView.centerYAnchor)
            ])

            context.coordinator.bannerView = bannerView

            // Load ad with root view controller
            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
               let rootVC = windowScene.windows.first?.rootViewController {
                bannerView.rootViewController = rootVC
                bannerView.load(GADRequest())
            }
        }
    }

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    class Coordinator: NSObject, GADBannerViewDelegate {
        var bannerView: GADBannerView?

        func bannerViewDidReceiveAd(_ bannerView: GADBannerView) {
            print("BannerAdView: Ad received")

            // Animate the banner in
            bannerView.alpha = 0
            UIView.animate(withDuration: 0.3) {
                bannerView.alpha = 1
            }
        }

        func bannerView(_ bannerView: GADBannerView, didFailToReceiveAdWithError error: Error) {
            print("BannerAdView: Failed to receive ad - \(error.localizedDescription)")
        }
    }
}

/// View modifier to add a banner ad at the bottom of a view
struct BannerAdModifier: ViewModifier {
    @ObservedObject var storeKitManager = StoreKitManager.shared

    func body(content: Content) -> some View {
        VStack(spacing: 0) {
            content

            if !storeKitManager.hasRemovedAds {
                BannerAdView()
                    .frame(height: 50)
            }
        }
    }
}

extension View {
    /// Add a banner ad at the bottom of this view
    func withBannerAd() -> some View {
        modifier(BannerAdModifier())
    }
}
