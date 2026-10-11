package com.mrdev.tashtech.domain.model

enum class LessonType {
    Lecture, Practical;

    companion object {
        /** Unknown or empty values give null: the lesson still shows, just without a badge. */
        fun parse(raw: String): LessonType? = when (raw.trim().lowercase()) {
            "lecture", "lec", "maruza" -> Lecture
            "practical", "practice", "lab", "seminar", "amaliy" -> Practical
            else -> null
        }
    }
}