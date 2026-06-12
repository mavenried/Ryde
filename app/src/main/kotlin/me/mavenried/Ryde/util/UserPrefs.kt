package me.mavenried.Ryde.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UserPrefs {
    private const val PREFS_NAME = "user_prefs"
    private const val KEY_WEIGHT_KG = "weight_kg"
    private const val KEY_USE_LBS = "use_lbs"
    private const val KEY_ONBOARDED = "onboarded"
    private const val KEY_THEME = "theme"

    private val _themeFlow = MutableStateFlow("system")
    val themeFlow: StateFlow<String> = _themeFlow.asStateFlow()

    fun initTheme(context: Context) {
        _themeFlow.value = getTheme(context)
    }

    fun getTheme(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, "system") ?: "system"

    fun setTheme(context: Context, theme: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, theme).apply()
        _themeFlow.value = theme
    }

    fun getWeightKg(context: Context): Double =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getFloat(KEY_WEIGHT_KG, 70f).toDouble()

    fun setWeightKg(context: Context, kg: Double) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putFloat(KEY_WEIGHT_KG, kg.toFloat()).apply()

    fun useLbs(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_USE_LBS, false)

    fun setUseLbs(context: Context, useLbs: Boolean) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_USE_LBS, useLbs).apply()

    fun isOnboarded(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDED, false)

    fun setOnboarded(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ONBOARDED, true).apply()

    fun kgToLbs(kg: Double) = kg * 2.20462
    fun lbsToKg(lbs: Double) = lbs / 2.20462
}
