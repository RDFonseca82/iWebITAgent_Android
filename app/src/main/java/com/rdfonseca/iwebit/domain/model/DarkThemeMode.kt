package com.rdfonseca.iwebit.domain.model

import androidx.annotation.Keep

@Keep
enum class DarkThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    fun isDarkTheme(
        isSystemInDarkTheme: Boolean,
    ): Boolean {
        return when (this) {
            SYSTEM -> isSystemInDarkTheme
            DARK -> true
            LIGHT -> false
        }
    }

//    fun nameTranslate(context: Context): String {
//        return when (this) {
//            SYSTEM -> context.getString(R.string.system)
//            DARK -> context.getString(R.string.dark)
//            LIGHT -> context.getString(R.string.light)
//        }
//    }
}
