package com.shijian.app.data.prefs

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 休息模式 */
enum class RestMode(val label: String, val weekdays: Set<Int>) {
    DOUBLE_REST("双休", setOf(1, 2, 3, 4, 5)),
    SINGLE_REST("单休", setOf(1, 2, 3, 4, 5, 6)),
    NO_REST("不休", setOf(1, 2, 3, 4, 5, 6, 7));

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: DOUBLE_REST
    }
}

/** 深色模式 */
enum class DarkMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色");

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

/** 应用设置（3.7，域模型） */
data class AppSettings(
    val workStartHour: Int = 9,
    val workStartMinute: Int = 0,
    val workEndHour: Int = 18,
    val workEndMinute: Int = 0,
    val restMode: RestMode = RestMode.DOUBLE_REST,
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val reimburseReminderEnabled: Boolean = true,
    val userName: String = "时笺用户",
    val searchRadiusKm: Int = 2,
    val multiPointSearch: Boolean = true
)

/**
 * 普通设置存储（Key 之外的配置）
 * 所有 Key 一律走 [SecurePrefs]，本类只存非敏感配置
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("shijian_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings

    private fun load(): AppSettings = AppSettings(
        workStartHour = prefs.getInt("work_start_hour", 9),
        workStartMinute = prefs.getInt("work_start_minute", 0),
        workEndHour = prefs.getInt("work_end_hour", 18),
        workEndMinute = prefs.getInt("work_end_minute", 0),
        restMode = RestMode.from(prefs.getString("rest_mode", null)),
        darkMode = DarkMode.from(prefs.getString("dark_mode", null)),
        reimburseReminderEnabled = prefs.getBoolean("reimburse_reminder", true),
        userName = prefs.getString("user_name", "时笺用户") ?: "时笺用户",
        searchRadiusKm = prefs.getInt("search_radius_km", 2),
        multiPointSearch = prefs.getBoolean("multi_point_search", true)
    )

    private fun update(block: SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply {
            block()
            apply()
        }
        _settings.value = load()
    }

    fun setWorkStart(hour: Int, minute: Int) = update {
        putInt("work_start_hour", hour); putInt("work_start_minute", minute)
    }

    fun setWorkEnd(hour: Int, minute: Int) = update {
        putInt("work_end_hour", hour); putInt("work_end_minute", minute)
    }

    fun setRestMode(mode: RestMode) = update { putString("rest_mode", mode.name) }

    fun setDarkMode(mode: DarkMode) = update { putString("dark_mode", mode.name) }

    fun setReimburseReminder(enabled: Boolean) = update { putBoolean("reimburse_reminder", enabled) }

    fun setUserName(name: String) = update { putString("user_name", name) }

    fun setSearchRadiusKm(km: Int) = update { putInt("search_radius_km", km) }

    fun setMultiPointSearch(enabled: Boolean) = update { putBoolean("multi_point_search", enabled) }

    /** 已看到过的版本号（更新公告用） */
    fun lastSeenVersion(): String? = prefs.getString("last_seen_version", null)

    fun markVersionSeen(version: String) {
        prefs.edit().putString("last_seen_version", version).apply()
    }
}
