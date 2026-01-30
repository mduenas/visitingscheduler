import Foundation
import StoreKit

/// Manager for StoreKit in-app purchases.
/// Handles ad removal purchase using StoreKit 2 (async/await).
@MainActor
class StoreKitManager: ObservableObject {

    /// Product ID for removing ads (must match App Store Connect)
    static let removeAdsProductId = "com.markduenas.visischeduler.remove_ads"

    /// Published state for UI binding
    @Published private(set) var hasRemovedAds: Bool = false
    @Published private(set) var removeAdsProduct: Product?
    @Published private(set) var purchaseState: PurchaseState = .idle
    @Published private(set) var errorMessage: String?

    /// Singleton instance for easy access
    static let shared = StoreKitManager()

    private var updateListenerTask: Task<Void, Error>?

    init() {
        // Start listening for transaction updates
        updateListenerTask = listenForTransactions()

        // Load products and check existing purchases
        Task {
            await loadProducts()
            await updatePurchasedProducts()
        }
    }

    deinit {
        updateListenerTask?.cancel()
    }

    // MARK: - Product Loading

    /// Load available products from App Store
    func loadProducts() async {
        do {
            let products = try await Product.products(for: [Self.removeAdsProductId])
            removeAdsProduct = products.first
        } catch {
            print("Failed to load products: \(error)")
            errorMessage = "Failed to load products"
        }
    }

    /// Get formatted price for ad removal
    var formattedPrice: String? {
        removeAdsProduct?.displayPrice
    }

    // MARK: - Purchase Flow

    /// Purchase ad removal
    func purchaseAdRemoval() async -> Bool {
        guard let product = removeAdsProduct else {
            errorMessage = "Product not available"
            purchaseState = .error
            return false
        }

        purchaseState = .purchasing

        do {
            let result = try await product.purchase()

            switch result {
            case .success(let verification):
                // Check if the transaction is verified
                switch verification {
                case .verified(let transaction):
                    // Grant access and finish the transaction
                    hasRemovedAds = true
                    await transaction.finish()
                    purchaseState = .purchased
                    return true

                case .unverified(_, let error):
                    errorMessage = "Purchase verification failed: \(error.localizedDescription)"
                    purchaseState = .error
                    return false
                }

            case .userCancelled:
                purchaseState = .cancelled
                return false

            case .pending:
                purchaseState = .pending
                return false

            @unknown default:
                purchaseState = .error
                return false
            }
        } catch {
            errorMessage = "Purchase failed: \(error.localizedDescription)"
            purchaseState = .error
            return false
        }
    }

    // MARK: - Restore Purchases

    /// Restore previous purchases
    func restorePurchases() async -> Bool {
        do {
            try await AppStore.sync()
            await updatePurchasedProducts()
            return hasRemovedAds
        } catch {
            errorMessage = "Failed to restore purchases: \(error.localizedDescription)"
            return false
        }
    }

    // MARK: - Transaction Handling

    /// Check current entitlements and update state
    func updatePurchasedProducts() async {
        for await result in Transaction.currentEntitlements {
            switch result {
            case .verified(let transaction):
                if transaction.productID == Self.removeAdsProductId {
                    hasRemovedAds = true
                    return
                }
            case .unverified:
                continue
            }
        }
        // If we get here, no valid purchase was found
        hasRemovedAds = false
    }

    /// Listen for transaction updates (renewals, refunds, etc.)
    private func listenForTransactions() -> Task<Void, Error> {
        return Task.detached {
            for await result in Transaction.updates {
                await self.handleTransaction(result)
            }
        }
    }

    private func handleTransaction(_ result: VerificationResult<Transaction>) async {
        switch result {
        case .verified(let transaction):
            if transaction.productID == Self.removeAdsProductId {
                if transaction.revocationDate != nil {
                    // Purchase was refunded
                    hasRemovedAds = false
                } else {
                    hasRemovedAds = true
                }
                await transaction.finish()
            }
        case .unverified:
            break
        }
    }
}

// MARK: - Purchase State

enum PurchaseState {
    case idle
    case purchasing
    case purchased
    case cancelled
    case pending
    case error
}
