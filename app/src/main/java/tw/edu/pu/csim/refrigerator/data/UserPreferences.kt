package tw.edu.pu.csim.refrigerator.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore("user_prefs")

object UserPreferences {
    private val USER_NAME_KEY = stringPreferencesKey("user_name")
    private val IMAGE_URI_KEY = stringPreferencesKey("image_uri")

    suspend fun saveUserName(context: Context, name: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_NAME_KEY] = name
        }
    }

    suspend fun loadUserName(context: Context): String? {
        return context.dataStore.data
            .map { it[USER_NAME_KEY] }
            .first()
    }
    suspend fun saveImageUri(context: Context, uri: String) {
        context.dataStore.edit { prefs ->
            prefs[IMAGE_URI_KEY] = uri
        }
    }

    suspend fun loadImageUri(context: Context): String? {
        return context.dataStore.data.map { it[IMAGE_URI_KEY] }.first()
    }
}
