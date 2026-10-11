package com.mrdev.tashtech.domain.student

object StudentIds {
    const val ID_LENGTH = 10 // "TTB" + 7 digits
    const val GROUP_PREFIX = "BS26-"
    const val EMAIL_DOMAIN = "tashkenttech-edu.uz"
    private val FORMAT = Regex("^TTB[0-9]{7}$")

    /** Uppercases and keeps only A–Z and 0–9: spaces, dashes and stray characters disappear. */
    fun normalize(raw: String): String =
        raw.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }

    /** What the text field shows while typing. */
    fun sanitize(raw: String): String = normalize(raw).take(ID_LENGTH)

    fun isValidFormat(id: String): Boolean = FORMAT.matches(id)

    fun timetableGroup(groupNumber: Int): String = "$GROUP_PREFIX$groupNumber"

    fun email(id: String): String = "${id.lowercase()}@$EMAIL_DOMAIN"
}