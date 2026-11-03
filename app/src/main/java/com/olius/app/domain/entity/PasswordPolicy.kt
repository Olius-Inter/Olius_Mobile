package com.olius.app.domain.entity

/** Regras da senha iguais às da Core (`StrongPasswordValidator`). */
enum class PasswordRule(val message: String) {
    LENGTH("De 10 a 128 caracteres"),
    UPPERCASE("Uma letra maiúscula"),
    DIGIT("Um número"),
    SYMBOL("Um símbolo (ex.: ! @ # \$)");

    fun isSatisfiedBy(password: String): Boolean = when (this) {
        LENGTH -> password.codePointCount(0, password.length) in MIN_LENGTH..MAX_LENGTH
        UPPERCASE -> password.codePoints().anyMatch(Character::isUpperCase)
        DIGIT -> password.codePoints().anyMatch(Character::isDigit)
        SYMBOL -> password.codePoints().anyMatch { !Character.isLetterOrDigit(it) && !Character.isWhitespace(it) }
    }

    companion object {
        const val MIN_LENGTH = 10
        const val MAX_LENGTH = 128

        /** Regras que a senha ainda não cumpre (vazia = senha aceita pela Core). */
        fun unmet(password: String): List<PasswordRule> = entries.filterNot { it.isSatisfiedBy(password) }
    }
}
