package com.example.flow

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
	private const val PREFS_NAME = "app_prefs"
	private const val KEY_THEME_MODE = "theme_mode"

	enum class ThemeMode(val modeValue: Int) {
		SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
		LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
		DARK(AppCompatDelegate.MODE_NIGHT_YES)
	}

	fun applySavedTheme(context: Context) {
		val mode = getSavedTheme(context)
		AppCompatDelegate.setDefaultNightMode(mode.modeValue)
	}

	fun saveAndApplyTheme(context: Context, themeMode: ThemeMode) {
		context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
			.edit()
			.putString(KEY_THEME_MODE, themeMode.name)
			.apply()
		AppCompatDelegate.setDefaultNightMode(themeMode.modeValue)
	}

	fun getSavedTheme(context: Context): ThemeMode {
		val name = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
			.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
		return runCatching { ThemeMode.valueOf(name!!) }.getOrElse { ThemeMode.SYSTEM }
	}
}


