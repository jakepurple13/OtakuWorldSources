package eu.kanade.tachiyomi.network

import android.content.Context
import android.os.Build
import android.webkit.WebSettings
import eu.kanade.tachiyomi.util.system.DeviceUtil
import tachiyomi.core.preference.Preference
import tachiyomi.core.preference.PreferenceStore

class NetworkPreferences(
    private val preferenceStore: PreferenceStore,
    private val context: Context,
    private val verboseLogging: Boolean = false,
) {

    fun verboseLogging(): Preference<Boolean> {
        return preferenceStore.getBoolean("verbose_logging", verboseLogging)
    }

    fun dohProvider(): Preference<Int> {
        return preferenceStore.getInt("doh_provider", 1)
    }

    fun defaultUserAgent(): Preference<String> {
        return preferenceStore.getString("default_user_agent", deviceDefaultUserAgent)
    }

    // Mirrors real Mihon: use the device's actual WebView/Chrome user agent instead of a
    // hardcoded string that inevitably goes stale and can trigger UA-based anti-bot blocking
    // on sites that check for a current, real browser signature.
    private val deviceDefaultUserAgent: String by lazy {
        // Same known-crashy-device guard as WebViewInterceptor's initWebView.
        // See https://bugs.chromium.org/p/chromium/issues/detail?id=1279562
        if (DeviceUtil.isMiui || (Build.VERSION.SDK_INT == Build.VERSION_CODES.S && DeviceUtil.isSamsung)) {
            return@lazy FALLBACK_USER_AGENT
        }

        try {
            WebSettings.getDefaultUserAgent(context)
        } catch (_: Exception) {
            // Avoid some crashes like when Chrome/WebView is being updated.
            FALLBACK_USER_AGENT
        }
    }

    companion object {
        private const val FALLBACK_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
    }
}
