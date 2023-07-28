package eu.kanade.tachiyomi.core.security

import tachiyomi.core.preference.PreferenceStore
import tachiyomi.core.preference.getEnum

class SecurityPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun useAuthenticator() = preferenceStore.getBoolean("use_biometric_lock", false)

    fun lockAppAfter() = preferenceStore.getInt("lock_app_after", 0)

    fun secureScreen() = preferenceStore.getEnum("secure_screen_v2", SecureScreenMode.INCOGNITO)

    fun hideNotificationContent() = preferenceStore.getBoolean("hide_notification_content", false)

    /**
     * For app lock. Will be set when there is a pending timed lock.
     * Otherwise this pref should be deleted.
     */
    fun lastAppClosed() = preferenceStore.getLong("last_app_closed", 0)

    enum class SecureScreenMode(val titleResId: Int) {
        ALWAYS(android.R.string.dialog_alert_title),
        INCOGNITO(android.R.string.dialog_alert_title),
        NEVER(android.R.string.dialog_alert_title),
    }
}
