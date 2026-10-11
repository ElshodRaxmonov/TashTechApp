package com.mrdev.tashtech.domain.model

import java.time.LocalDateTime

const val MAX_NOTE_TEXT = 500

data class Note(val id: Long, val text: String, val sourceUrl: String, val createdAt: LocalDateTime)