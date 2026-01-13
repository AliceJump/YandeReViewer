package com.alicejump.yandeviewer.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alicejump.yandeviewer.network.GitHubApiClient
import com.alicejump.yandeviewer.network.GitHubRelease
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Sealed interface for representing the state of an update check.
 */
sealed interface UpdateCheckState {
    object Idle : UpdateCheckState
    object Checking : UpdateCheckState
    data class UpdateAvailable(val release: GitHubRelease) : UpdateCheckState
    object NoUpdate : UpdateCheckState
    data class Error(val message: String) : UpdateCheckState
}

class UpdateViewModel : ViewModel() {

    private val _updateState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateState: StateFlow<UpdateCheckState> = _updateState

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
    }

    fun checkForUpdate(context: Context, owner: String, repo: String) {
        val currentVersion = getCurrentVersion(context)
        if (currentVersion.endsWith("-local")) {
            _updateState.value = UpdateCheckState.NoUpdate // Don't check for updates on local builds
            return
        }

        viewModelScope.launch {
            _updateState.value = UpdateCheckState.Checking
            try {
                val latestRelease = GitHubApiClient.api.getLatestRelease(owner, repo)
                val latestVersionName = latestRelease.tagName.removePrefix("v")

                // Check user preferences
                val prefs = getPrefs(context)
                val ignoredVersion = prefs.getString("ignored_version", null)
                val snoozeUntil = prefs.getLong("snooze_until", 0)

                if (ignoredVersion == latestVersionName) {
                    _updateState.value = UpdateCheckState.NoUpdate
                    return@launch
                }
                if (System.currentTimeMillis() < snoozeUntil) {
                    _updateState.value = UpdateCheckState.NoUpdate
                    return@launch
                }

                if (isNewerVersion(latestVersionName, currentVersion)) {
                    _updateState.value = UpdateCheckState.UpdateAvailable(latestRelease)
                } else {
                    _updateState.value = UpdateCheckState.NoUpdate
                }
            } catch (e: Exception) {
                _updateState.value = UpdateCheckState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun snoozeUpdate(context: Context, days: Int) {
        val snoozeUntil = System.currentTimeMillis() + days * 24 * 60 * 60 * 1000L
        getPrefs(context).edit {
            putLong("snooze_until", snoozeUntil)
        }
    }

    fun ignoreThisVersion(context: Context, version: String) {
        getPrefs(context).edit {
            putString("ignored_version", version)
        }
    }

    private fun getCurrentVersion(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
        } catch (_: PackageManager.NameNotFoundException) {
            "0.0.0" // Should not happen
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        // This is a simplified version comparison. For production apps, a more robust
        // version comparison logic (handling more complex version strings) is recommended.
        return latest > current
    }
}
