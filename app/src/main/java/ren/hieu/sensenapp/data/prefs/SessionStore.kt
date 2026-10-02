package ren.hieu.sensenapp.data.prefs

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "sensen_session",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var isLinked: Boolean
        get() = prefs.getBoolean(KEY_LINKED, false)
        set(value) = prefs.edit().putBoolean(KEY_LINKED, value).apply()

    var school: String?
        get() = prefs.getString(KEY_SCHOOL, null)
        set(value) = prefs.edit().putString(KEY_SCHOOL, value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_LINKED = "linked"
        private const val KEY_SCHOOL = "school"
    }
}
