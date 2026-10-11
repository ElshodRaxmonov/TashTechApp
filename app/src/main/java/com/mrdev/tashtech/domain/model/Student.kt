package com.mrdev.tashtech.domain.model

import com.mrdev.tashtech.domain.student.StudentIds

/** 0 = female, 1 = male (as in students.json). Anything else is kept as Unspecified. */
enum class Sex(val code: Int) {
    Female(0), Male(1), Unspecified(-1);

    companion object {
        fun fromCode(code: Int?): Sex = entries.firstOrNull { it.code == code } ?: Unspecified
    }
}

data class Student(
    val id: String,
    val name: String,
    val groupNumber: Int,
    val sex: Sex,
) {
    /** Key into timetable.json, e.g. 3 → "BS26-3". */
    val timetableGroup: String get() = StudentIds.timetableGroup(groupNumber)
    val email: String get() = StudentIds.email(id)
}