package com.qingjizhang.app.data

enum class AppLanguage(val storageKey: String) {
    SYSTEM("system"),
    ZH("zh"),
    EN("en"),
    ;

    companion object {
        fun fromStorage(raw: String?): AppLanguage =
            entries.find { it.storageKey == raw } ?: SYSTEM
    }
}

enum class ThemeMode(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    ;

    companion object {
        fun fromStorage(raw: String?): ThemeMode =
            entries.find { it.storageKey == raw } ?: SYSTEM

        /** Migrate legacy boolean dark-theme preference. */
        fun fromLegacyDarkFlag(dark: Boolean): ThemeMode = if (dark) DARK else LIGHT
    }
}
