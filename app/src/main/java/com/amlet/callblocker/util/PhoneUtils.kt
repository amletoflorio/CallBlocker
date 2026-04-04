package com.amlet.callblocker.util

object PhoneUtils {

    /**
     * Normalizes a phone number by stripping everything that is not a digit.
     * Also removes redundant leading zeros (e.g. 0039 → 39).
     *
     * Examples:
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
     * Formats a number for display, recognising the most common international
     * prefixes (IT, DE, FR, ES, US/CA, UK, etc.).
     *
     * If the number starts with "+" or has a recognisable prefix it is
     * formatted according to the country's conventions. Otherwise returned as-is.
     */
    fun formatForDisplay(number: String): String {
        val digits = number.replace(Regex("[^0-9+]"), "")

        // Normalise "00" prefix → "+"
        val withPlus = when {
            digits.startsWith("00") -> "+${digits.drop(2)}"
            digits.startsWith("+")  -> digits
            // Bare country code — prepend "+" so the rest of the logic works.
            // Covers numbers stored without "+" (e.g. "390230612645" → "+390230612645").
            else -> "+$digits"
        }

        return when {
            withPlus.startsWith("+39") -> formatIT(withPlus.drop(3))   // Italy
            withPlus.startsWith("+49") -> formatDE(withPlus.drop(3))   // Germany
            withPlus.startsWith("+33") -> formatFR(withPlus.drop(3))   // France
            withPlus.startsWith("+34") -> formatES(withPlus.drop(3))   // Spain
            withPlus.startsWith("+1")  -> formatNANP(withPlus.drop(2)) // USA/Canada
            withPlus.startsWith("+44") -> formatUK(withPlus.drop(3))   // UK
            withPlus.startsWith("+")   -> formatGeneric(withPlus)       // Other international
            else -> number
        }
    }

    // ── Country formatters ────────────────────────────────────────────────────

    /** IT: +39 02 1234 5678 / +39 333 123 4567 */
    private fun formatIT(local: String): String {
        val d = local.trimStart('0')
        return when {
            // Mobile (3xx): +39 3XX XXX XXXX
            d.length == 10 && d.startsWith("3") ->
                "+39 ${d.take(3)} ${d.drop(3).take(3)} ${d.drop(6)}"
            // Landline (02/06 + 8 digits): +39 02 XXXX XXXX
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

    /** Generic international: groups into blocks of 3 */
    private fun formatGeneric(number: String): String {
        val prefix = number.takeWhile { it == '+' || it.isDigit() && number.indexOf(it) < 4 }
        val rest = number.drop(prefix.length)
        return "$prefix ${rest.chunked(3).joinToString(" ")}".trim()
    }

    /** Validates that a string contains at least 6 digits (minimum valid number). */
    fun isValid(number: String): Boolean {
        return normalize(number).length >= 6
    }

    // ── Number info (offline, prefix-based) ──────────────────────────────────

    data class NumberInfo(
        val formattedNumber: String,
        val country: String,
        val operator: String?,
        val numberType: NumberType
    )

    enum class NumberType {
        MOBILE, LANDLINE, VOIP, TOLL_FREE, UNKNOWN
    }

    /**
     * Returns all locally derivable information about a number using only
     * offline prefix tables — no network calls are made.
     */
    fun getNumberInfo(rawNumber: String): NumberInfo {
        val formatted = formatForDisplay(rawNumber)
        val digits = rawNumber.replace(Regex("[^0-9+]"), "")
        // Same normalisation as formatForDisplay: always ensure a "+" prefix so
        // bare country-code numbers (e.g. "390230612645") are matched correctly.
        val normalized = when {
            digits.startsWith("00") -> "+${digits.drop(2)}"
            digits.startsWith("+")  -> digits
            else                    -> "+$digits"
        }

        return when {
            normalized.startsWith("+39") -> analyzeIT(normalized, formatted)
            normalized.startsWith("+49") -> analyzeDE(normalized, formatted)
            normalized.startsWith("+33") -> analyzeFR(normalized, formatted)
            normalized.startsWith("+34") -> analyzeES(normalized, formatted)
            normalized.startsWith("+1")  -> analyzeNANP(normalized, formatted)
            normalized.startsWith("+44") -> analyzeUK(normalized, formatted)
            normalized.startsWith("+800") || normalized.startsWith("+808") ->
                NumberInfo(formatted, "International", null, NumberType.TOLL_FREE)
            normalized.startsWith("+") -> analyzeGenericInternational(normalized, formatted)
            else -> NumberInfo(formatted, "Unknown", null, NumberType.UNKNOWN)
        }
    }

    // ── Country-specific analysers ────────────────────────────────────────────

    private fun analyzeIT(e164: String, formatted: String): NumberInfo {
        val local = e164.drop(3).trimStart('0')
        val type = when {
            local.startsWith("3")                                       -> NumberType.MOBILE
            local.startsWith("800") || local.startsWith("803")         -> NumberType.TOLL_FREE
            local.startsWith("848") || local.startsWith("847")         -> NumberType.TOLL_FREE
            local.startsWith("0")                                       -> NumberType.LANDLINE
            local.startsWith("43")                                      -> NumberType.VOIP
            else                                                        -> NumberType.UNKNOWN
        }
        val operator = when {
            local.startsWith("320") || local.startsWith("330") || local.startsWith("340") ||
                local.startsWith("348") || local.startsWith("349") -> "Vodafone"
            local.startsWith("333") || local.startsWith("334") ||
                local.startsWith("335") || local.startsWith("336") -> "TIM"
            local.startsWith("366") || local.startsWith("380") ||
                local.startsWith("388") || local.startsWith("389") -> "Wind Tre"
            local.startsWith("391") || local.startsWith("392") ||
                local.startsWith("393") || local.startsWith("398") -> "Iliad"
            local.startsWith("351") || local.startsWith("352") ||
                local.startsWith("353") -> "PosteMobile"
            else -> null
        }
        return NumberInfo(formatted, "Italy", operator, type)
    }

    private fun analyzeDE(e164: String, formatted: String): NumberInfo {
        val local = e164.drop(3).trimStart('0')
        val type = when {
            local.startsWith("15") || local.startsWith("16") || local.startsWith("17") -> NumberType.MOBILE
            local.startsWith("800")                                                      -> NumberType.TOLL_FREE
            local.startsWith("032")                                                      -> NumberType.VOIP
            else                                                                         -> NumberType.LANDLINE
        }
        val operator = when {
            local.startsWith("151") || local.startsWith("160") || local.startsWith("170") ||
                local.startsWith("171") -> "Telekom"
            local.startsWith("152") || local.startsWith("162") || local.startsWith("172") -> "Vodafone DE"
            local.startsWith("155") || local.startsWith("157") || local.startsWith("163") ||
                local.startsWith("177") || local.startsWith("178") -> "O2 Germany"
            else -> null
        }
        return NumberInfo(formatted, "Germany", operator, type)
    }

    private fun analyzeFR(e164: String, formatted: String): NumberInfo {
        val local = e164.drop(3).trimStart('0')
        val type = when {
            local.startsWith("6") || local.startsWith("7") -> NumberType.MOBILE
            local.startsWith("800") || local.startsWith("805") -> NumberType.TOLL_FREE
            local.startsWith("9")                              -> NumberType.VOIP
            else                                               -> NumberType.LANDLINE
        }
        return NumberInfo(formatted, "France", null, type)
    }

    private fun analyzeES(e164: String, formatted: String): NumberInfo {
        val local = e164.drop(3).trimStart('0')
        val type = when {
            local.startsWith("6") || local.startsWith("7") -> NumberType.MOBILE
            local.startsWith("800") || local.startsWith("900") -> NumberType.TOLL_FREE
            else                                                 -> NumberType.LANDLINE
        }
        return NumberInfo(formatted, "Spain", null, type)
    }

    private fun analyzeNANP(e164: String, formatted: String): NumberInfo {
        val local = e164.drop(2)
        val areaCode = local.take(3)
        // NANP toll-free area codes
        val tollFreeAreaCodes = setOf("800", "833", "844", "855", "866", "877", "888")
        val type = if (areaCode in tollFreeAreaCodes) NumberType.TOLL_FREE else NumberType.LANDLINE
        val country = when (areaCode.take(1)) {
            "1" -> "USA/Canada"
            else -> "USA/Canada"
        }
        return NumberInfo(formatted, country, null, type)
    }

    private fun analyzeUK(e164: String, formatted: String): NumberInfo {
        val local = e164.drop(3).trimStart('0')
        val type = when {
            local.startsWith("7")                               -> NumberType.MOBILE
            local.startsWith("800") || local.startsWith("808") -> NumberType.TOLL_FREE
            local.startsWith("56") || local.startsWith("70")   -> NumberType.VOIP
            else                                                -> NumberType.LANDLINE
        }
        return NumberInfo(formatted, "United Kingdom", null, type)
    }

    private fun analyzeGenericInternational(e164: String, formatted: String): NumberInfo {
        // Derive country from ITU-T country code prefix table (partial, most common).
        val country = when {
            e164.startsWith("+7")   -> "Russia / Kazakhstan"
            e164.startsWith("+20")  -> "Egypt"
            e164.startsWith("+27")  -> "South Africa"
            e164.startsWith("+30")  -> "Greece"
            e164.startsWith("+31")  -> "Netherlands"
            e164.startsWith("+32")  -> "Belgium"
            e164.startsWith("+36")  -> "Hungary"
            e164.startsWith("+38")  -> "Ukraine"
            e164.startsWith("+40")  -> "Romania"
            e164.startsWith("+41")  -> "Switzerland"
            e164.startsWith("+43")  -> "Austria"
            e164.startsWith("+45")  -> "Denmark"
            e164.startsWith("+46")  -> "Sweden"
            e164.startsWith("+47")  -> "Norway"
            e164.startsWith("+48")  -> "Poland"
            e164.startsWith("+51")  -> "Peru"
            e164.startsWith("+52")  -> "Mexico"
            e164.startsWith("+54")  -> "Argentina"
            e164.startsWith("+55")  -> "Brazil"
            e164.startsWith("+56")  -> "Chile"
            e164.startsWith("+57")  -> "Colombia"
            e164.startsWith("+60")  -> "Malaysia"
            e164.startsWith("+61")  -> "Australia"
            e164.startsWith("+62")  -> "Indonesia"
            e164.startsWith("+63")  -> "Philippines"
            e164.startsWith("+64")  -> "New Zealand"
            e164.startsWith("+65")  -> "Singapore"
            e164.startsWith("+66")  -> "Thailand"
            e164.startsWith("+81")  -> "Japan"
            e164.startsWith("+82")  -> "South Korea"
            e164.startsWith("+84")  -> "Vietnam"
            e164.startsWith("+86")  -> "China"
            e164.startsWith("+90")  -> "Turkey"
            e164.startsWith("+91")  -> "India"
            e164.startsWith("+92")  -> "Pakistan"
            e164.startsWith("+93")  -> "Afghanistan"
            e164.startsWith("+94")  -> "Sri Lanka"
            e164.startsWith("+98")  -> "Iran"
            else -> "International"
        }
        return NumberInfo(formatted, country, null, NumberType.UNKNOWN)
    }
}
