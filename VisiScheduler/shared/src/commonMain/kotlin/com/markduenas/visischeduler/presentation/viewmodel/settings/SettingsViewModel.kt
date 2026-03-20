package com.markduenas.visischeduler.presentation.viewmodel.settings

import com.markduenas.visischeduler.domain.entities.Role
import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.domain.repository.UserRepository
import com.markduenas.visischeduler.platform.SecureStorage
import com.markduenas.visischeduler.platform.UrlOpener
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * App theme options.
 */
enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * App language options.
 */
enum class AppLanguage(val displayName: String, val code: String) {
    ENGLISH("English", "en"),
    SPANISH("Espanol", "es"),
    FRENCH("Francais", "fr"),
    GERMAN("Deutsch", "de"),
    CHINESE("Chinese", "zh"),
    JAPANESE("Japanese", "ja")
}

/**
 * UI state for the Settings screen.
 */
data class SettingsUiState(
    val currentUser: User? = null,
    val notificationsEnabled: Boolean = true,
    val biometricEnabled: Boolean = false,
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val textScale: Float = 1.0f,
    val isLoading: Boolean = false,
    val appVersion: String = "1.0.0"
) {
    val isCoordinator: Boolean
        get() = currentUser?.role in listOf(
            Role.ADMIN,
            Role.PRIMARY_COORDINATOR,
            Role.SECONDARY_COORDINATOR
        )
}

/**
 * ViewModel for managing app settings.
 */
class SettingsViewModel(
    private val secureStorage: SecureStorage,
    private val userRepository: UserRepository,
    private val authRepository: com.markduenas.visischeduler.domain.repository.AuthRepository,
    private val urlOpener: UrlOpener
) : BaseViewModel<SettingsUiState>(SettingsUiState()) {

    companion object {
        private const val KEY_NOTIFICATIONS_ENABLED = "settings_notifications_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "settings_biometric_enabled"
        private const val KEY_THEME = "settings_theme"
        private const val KEY_LANGUAGE = "settings_language"
        private const val KEY_TEXT_SCALE = "settings_text_scale"
    }

    init {
        loadSettings()
        observeCurrentUser()
    }

    /**
     * Observe current user changes.
     */
    private fun observeCurrentUser() {
        viewModelScope.launch {
            userRepository.currentUser.collectLatest { user ->
                updateState { copy(currentUser = user) }
            }
        }
    }

    /**
     * Load all settings from secure storage.
     */
    private fun loadSettings() {
        updateState { copy(isLoading = true) }

        val notificationsEnabled = secureStorage.getBoolean(KEY_NOTIFICATIONS_ENABLED) ?: true
        val biometricEnabled = secureStorage.getBoolean(KEY_BIOMETRIC_ENABLED) ?: false
        val themeValue = secureStorage.getString(KEY_THEME) ?: AppTheme.SYSTEM.name
        val languageValue = secureStorage.getString(KEY_LANGUAGE) ?: AppLanguage.ENGLISH.code
        val textScale = secureStorage.getString(KEY_TEXT_SCALE)?.toFloatOrNull() ?: 1.0f

        val theme = try {
            AppTheme.valueOf(themeValue)
        } catch (e: Exception) {
            AppTheme.SYSTEM
        }

        val language = AppLanguage.entries.find { it.code == languageValue } ?: AppLanguage.ENGLISH

        updateState {
            copy(
                notificationsEnabled = notificationsEnabled,
                biometricEnabled = biometricEnabled,
                theme = theme,
                language = language,
                textScale = textScale,
                isLoading = false
            )
        }
    }

    /**
     * Toggle notifications enabled state.
     */
    fun toggleNotifications(enabled: Boolean) {
        secureStorage.putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
        updateState { copy(notificationsEnabled = enabled) }
    }

    /**
     * Toggle biometric authentication.
     */
    fun toggleBiometric(enabled: Boolean) {
        secureStorage.putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
        updateState { copy(biometricEnabled = enabled) }
    }

    /**
     * Set app theme.
     */
    fun setTheme(theme: AppTheme) {
        secureStorage.putString(KEY_THEME, theme.name)
        updateState { copy(theme = theme) }
    }

    /**
     * Set app language.
     */
    fun setLanguage(language: AppLanguage) {
        secureStorage.putString(KEY_LANGUAGE, language.code)
        updateState { copy(language = language) }
    }

    /**
     * Set text scale factor.
     */
    fun setTextScale(scale: Float) {
        secureStorage.putString(KEY_TEXT_SCALE, scale.toString())
        updateState { copy(textScale = scale) }
    }

    /**
     * Navigate to profile screen.
     */
    fun navigateToProfile() {
        navigate("profile")
    }

    /**
     * Navigate to security settings screen.
     */
    fun navigateToSecuritySettings() {
        navigate("security_settings")
    }

    /**
     * Navigate to notification settings screen.
     */
    fun navigateToNotificationSettings() {
        navigate("notification_settings")
    }

    /**
     * Navigate to appearance settings screen.
     */
    fun navigateToAppearanceSettings() {
        navigate("appearance_settings")
    }

    /**
     * Navigate to beneficiary settings screen.
     */
    fun navigateToBeneficiarySettings() {
        navigate("beneficiary_settings")
    }

    /**
     * Navigate to about screen.
     */
    fun navigateToAbout() {
        navigate("about")
    }

    /**
     * Navigate to terms of service.
     */
    fun navigateToTermsOfService() {
        navigate("terms")
    }

    /**
     * Navigate to privacy policy.
     */
    fun navigateToPrivacyPolicy() {
        navigate("privacy")
    }


    /**
     * Open external link for support.
     */
    fun contactSupport() {
        urlOpener.openUrl("mailto:support@visischeduler.com?subject=VisiScheduler Support")
    }

    fun openLicenses() {
        urlOpener.openUrl("https://visischeduler.com/licenses")
    }

    fun rateApp() {
        urlOpener.openUrl("https://play.google.com/store/apps/details?id=com.markduenas.visischeduler")
    }

    /**
     * Logout the current user.
     */
    fun logout() {
        launchSafe {
            updateState { copy(isLoading = true) }
            authRepository.logout().fold(
                onSuccess = {
                    navigate("login")
                },
                onFailure = {
                    navigate("login")
                }
            )
        }
    }

    /**
     * Reset all settings to defaults.
     */
    fun resetToDefaults() {
        secureStorage.remove(KEY_NOTIFICATIONS_ENABLED)
        secureStorage.remove(KEY_BIOMETRIC_ENABLED)
        secureStorage.remove(KEY_THEME)
        secureStorage.remove(KEY_LANGUAGE)
        secureStorage.remove(KEY_TEXT_SCALE)
        loadSettings()
        showSnackbar("Settings reset to defaults")
    }
}
