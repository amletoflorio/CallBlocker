package com.amlet.callblocker.util

object PhoneUtils {

    /**
     * Normalizza un numero telefonico rimuovendo tutto ciò che non è una cifra.
     * Rimuove anche gli zeri iniziali ridondanti (es. 0039 → 39).
     *
     * Esempi:
     *   "+39 02 1234567"    → "390212345678"
     *   "02-123.456"        → "02123456"
     *   "(+1) 800-555-0199" → "18005550199"
     */
    fun normalize(number: String): String {
        return number
            .replace(Regex("[^0-9]"), "")
            .trimStart('0')
    }

    /**
     * Formatta un numero per la visualizzazione riconoscendo i prefissi
     * internazionali più comuni (IT, DE, FR, ES, US/CA, UK, ecc.).
     *
     * Se il numero inizia con "+" o ha un prefisso riconoscibile viene
     * formattato secondo le convenzioni del paese. Altrimenti viene
     * restituito così com'è.
     */
    fun formatForDisplay(number: String): String {
        val digits = number.replace(Regex("[^0-9+]"), "")

        // Normalizza eventuale prefisso "00" → "+"
        val normalized = if (digits.startsWith("00")) "+${digits.drop(2)}" else digits

        return when {
            // ── Internazionale con prefisso esplicito ────────────────────────
            normalized.startsWith("+39") -> formatIT(normalized.drop(3))   // Italia
            normalized.startsWith("+49") -> formatDE(normalized.drop(3))   // Germania
            normalized.startsWith("+33") -> formatFR(normalized.drop(3))   // Francia
            normalized.startsWith("+34") -> formatES(normalized.drop(3))   // Spagna
            normalized.startsWith("+1")  -> formatNANP(normalized.drop(2)) // USA/Canada
            normalized.startsWith("+44") -> formatUK(normalized.drop(3))   // UK
            normalized.startsWith("+")   -> formatGeneric(normalized)       // Altro internazionale

            // ── Numeri locali italiani (senza prefisso) ──────────────────────
            number.length in 9..10 && (number.startsWith("3") || number.startsWith("0")) ->
                formatIT(number)

            else -> number
        }
    }

    // ── Formattatori per paese ────────────────────────────────────────────────

    /** IT: +39 02 1234 5678 / +39 333 123 4567 */
    private fun formatIT(local: String): String {
        val d = local.trimStart('0')
        return when {
            // Cellulare (3xx): +39 3XX XXX XXXX
            d.length == 10 && d.startsWith("3") ->
                "+39 ${d.take(3)} ${d.drop(3).take(3)} ${d.drop(6)}"
            // Fisso Milano/Roma (02/06 + 8 cifre): +39 02 XXXX XXXX
            d.length >= 9 ->
                "+39 ${d.take(2)} ${d.drop(2).take(4)} ${d.drop(6)}"
            else -> "+39 $local"
        }
    }

    /** DE: +49 30 12345678 / +49 151 12345678 */
    private fun formatDE(local: String): String {
        val d = local.trimStart('0')
        return when {
            d.length == 11 -> "+49 ${d.take(3)} ${d.drop(3).take(4)} ${d.drop(7)}"
            d.length == 10 -> "+49 ${d.take(3)} ${d.drop(3).take(3)} ${d.drop(6)}"
            else           -> "+49 $local"
        }
    }

    /** FR: +33 6 12 34 56 78 */
    private fun formatFR(local: String): String {
        val d = local.trimStart('0')
        return if (d.length == 9)
            "+33 ${d.take(1)} ${d.drop(1).chunked(2).joinToString(" ")}"
        else
            "+33 $local"
    }

    /** ES: +34 612 345 678 */
    private fun formatES(local: String): String {
        val d = local.trimStart('0')
        return if (d.length == 9)
            "+34 ${d.take(3)} ${d.drop(3).take(3)} ${d.drop(6)}"
        else
            "+34 $local"
    }

    /** US/CA NANP: +1 (212) 555-0199 */
    private fun formatNANP(local: String): String {
        val d = local.trimStart('0')
        return if (d.length == 10)
            "+1 (${d.take(3)}) ${d.drop(3).take(3)}-${d.drop(6)}"
        else
            "+1 $local"
    }

    /** UK: +44 7911 123456 */
    private fun formatUK(local: String): String {
        val d = local.trimStart('0')
        return when {
            d.length == 10 -> "+44 ${d.take(4)} ${d.drop(4)}"
            d.length == 9  -> "+44 ${d.take(3)} ${d.drop(3)}"
            else           -> "+44 $local"
        }
    }

    /** Generico internazionale: raggruppa a blocchi di 3 */
    private fun formatGeneric(number: String): String {
        val prefix = number.takeWhile { it == '+' || it.isDigit() && number.indexOf(it) < 4 }
        val rest = number.drop(prefix.length)
        return "$prefix ${rest.chunked(3).joinToString(" ")}".trim()
    }

    /** Valida che una stringa contenga almeno 6 cifre (numero minimo valido) */
    fun isValid(number: String): Boolean {
        return normalize(number).length >= 6
    }
}
