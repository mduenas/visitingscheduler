import SwiftUI
import shared
import FirebaseCore
import FirebaseAnalytics
import FirebaseCrashlytics
import FirebaseFirestore
import GoogleMobileAds

/// AppDelegate for Firebase initialization
class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // Initialize Firebase
        FirebaseApp.configure()

        // Configure Analytics
        #if DEBUG
        Analytics.setAnalyticsCollectionEnabled(false)
        #else
        Analytics.setAnalyticsCollectionEnabled(true)
        #endif

        // Configure Crashlytics
        #if DEBUG
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(false)
        #else
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(true)
        #endif

        // Set custom keys for crash reports
        Crashlytics.crashlytics().setCustomValue(
            Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "unknown",
            forKey: "app_version"
        )
        Crashlytics.crashlytics().setCustomValue("ios", forKey: "platform")

        // Log app open event
        Analytics.logEvent(AnalyticsEventAppOpen, parameters: nil)

        print("Firebase initialized successfully")

        // Initialize Google Mobile Ads SDK
        MobileAds.shared.start { status in
            print("AdMob SDK initialized")

            // Log adapter statuses for debugging
            let adapterStatuses = status.adapterStatusesByClassName
            for (adapter, adapterStatus) in adapterStatuses {
                print("AdMob Adapter: \(adapter), State: \(adapterStatus.state.rawValue)")
            }
        }

        return true
    }
}

@main
struct iOSApp: App {
    // Register app delegate for Firebase setup
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
