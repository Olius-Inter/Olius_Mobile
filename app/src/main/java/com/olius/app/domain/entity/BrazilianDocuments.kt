package com.olius.app.domain.entity

/**
 * Confere CPF e CNPJ pelos dígitos verificadores. A Core só confere a
 * quantidade de dígitos (11 e 14); o app confere também os dígitos para
 * avisar o erro de digitação antes de enviar.
 */
object BrazilianDocuments {

    const val CPF_LENGTH = 11
    const val CNPJ_LENGTH = 14

    fun isValidCpf(digits: String): Boolean {
        if (digits.length != CPF_LENGTH || !digits.all(Char::isDigit) || digits.all { it == digits[0] }) return false
        val numbers = digits.map { it - '0' }
        val first = checkDigit(numbers.take(9), (10 downTo 2).toList())
        val second = checkDigit(numbers.take(10), (11 downTo 2).toList())
        return numbers[9] == first && numbers[10] == second
    }

    fun isValidCnpj(digits: String): Boolean {
        if (digits.length != CNPJ_LENGTH || !digits.all(Char::isDigit) || digits.all { it == digits[0] }) return false
        val numbers = digits.map { it - '0' }
        val first = checkDigit(numbers.take(12), listOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2))
        val second = checkDigit(numbers.take(13), listOf(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2))
        return numbers[12] == first && numbers[13] == second
    }

    /** 000.000.000-00 enquanto a pessoa digita. */
    fun formatCpf(digits: String): String = mask(digits, "###.###.###-##")

    /** 00.000.000/0000-00 enquanto a pessoa digita. */
    fun formatCnpj(digits: String): String = mask(digits, "##.###.###/####-##")

    private fun checkDigit(numbers: List<Int>, weights: List<Int>): Int {
        val rest = numbers.zip(weights).sumOf { (n, w) -> n * w } % 11
        return if (rest < 2) 0 else 11 - rest
    }

    // Preenche os "#" com os dígitos; para onde os dígitos acabam.
    private fun mask(digits: String, pattern: String): String {
        val out = StringBuilder()
        var index = 0
        for (c in pattern) {
            if (index >= digits.length) break
            if (c == '#') out.append(digits[index++]) else out.append(c)
        }
        return out.toString()
    }
}
