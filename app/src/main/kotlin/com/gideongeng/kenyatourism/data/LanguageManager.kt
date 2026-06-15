package com.gideongeng.kenyatourism.data

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LanguageManager {
    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "selected_language"
    private const val KEY_FIRST_LAUNCH = "first_launch"
    
    data class Language(
        val code: String,
        val displayName: String,
        val nativeName: String
    )
    
    val supportedLanguages = listOf(
        Language("en", "English", "English"),
        Language("sw", "Swahili", "Kiswahili"),
        Language("fr", "French", "Français"),
        Language("de", "German", "Deutsch"),
        Language("es", "Spanish", "Español"),
        Language("zh", "Chinese", "中文"),
        Language("ja", "Japanese", "日本語"),
        Language("ar", "Arabic", "العربية"),
        Language("pt", "Portuguese", "Português"),
        Language("it", "Italian", "Italiano"),
        Language("ko", "Korean", "한국어"),
        Language("hi", "Hindi", "हिन्दी")
    )
    
    fun isFirstLaunch(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }
    
    fun setFirstLaunchComplete(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }
    
    fun getSelectedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, "en") ?: "en"
    }
    
    fun setSelectedLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
        applyLanguage(context, languageCode)
    }
    
    fun applyLanguage(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
    
    fun wrapContext(context: Context): Context {
        val lang = getSelectedLanguage(context)
        return applyLanguage(context, lang)
    }
}
