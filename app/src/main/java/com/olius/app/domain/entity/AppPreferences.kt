package com.olius.app.domain.entity

/** Tema escolhido na tela de Perfil. */
enum class ThemeMode(val label: String) {
    LIGHT("Modo Claro"),
    DARK("Modo Escuro"),
    SYSTEM("Seguir o sistema");

    /** Tema escuro de fato, dado o modo do aparelho. */
    fun isDark(systemIsDark: Boolean): Boolean = when (this) {
        LIGHT -> false
        DARK -> true
        SYSTEM -> systemIsDark
    }

    companion object {
        val DEFAULT = LIGHT

        fun fromStored(value: String?): ThemeMode = entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}

/** Dados enviados no "Editar Perfil" (PATCH /users/me). */
data class ProfileUpdate(
    val name: String,
    val email: String,
    val telephone: String?
)
