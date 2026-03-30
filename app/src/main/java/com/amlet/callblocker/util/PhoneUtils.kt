package com.amlet.callblocker.util

object PhoneUtils {

    /**
     * Normalizza un numero telefonico rimuovendo tutto ciò che non è una cifra,
     * e il prefisso internazionale "+".
     *
     * Esempi:
     *   "+39 02 1234567"  → "390212345678"
     *   "02-123.456"      → "02123456"
     *   "(+1) 800-555-0199" → "18005550199"
     */
    fun normalize(number: String): String {
        return number
            .replace(Regex("[^0-9]"), "")  // Rimuove tutto tranne le cifre
            .trimStart('0')               // Rimuove gli zero iniziali (es. 0039 → 39)
    }

    /**
     * Formatta un numero per la visualizzazione.
     * Semplice ma efficace per la maggior parte dei casi italiani.
     */
    fun formatForDisplay(number: String): String {
        return if (number.length >= 10) {
            "+${number.take(2)} ${number.drop(2).chunked(3).joinToString(" ")}"
        } else {
            number
        }
    }

    /** Valida che una stringa contenga almeno 6 cifre (numero minimo valido) */
    fun isValid(number: String): Boolean {
        return normalize(number).length >= 6
    }
}