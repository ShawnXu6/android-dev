package com.example.android_dev.data

import android.content.Context
import com.example.android_dev.ui.theme.AppPalette

// 主题偏好功能：持久化用户选择的配色方案（粉蓝 / 青绿），全局共享、跨账户生效。
class ThemePreference(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("smart_todo_theme", Context.MODE_PRIVATE)

    fun load(): AppPalette = AppPalette.fromName(prefs.getString(KEY_PALETTE, null))

    fun save(palette: AppPalette) {
        prefs.edit().putString(KEY_PALETTE, palette.name).apply()
    }

    private companion object {
        const val KEY_PALETTE = "palette"
    }
}
