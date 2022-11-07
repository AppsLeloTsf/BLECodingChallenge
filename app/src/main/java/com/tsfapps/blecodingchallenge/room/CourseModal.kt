package com.tsfapps.blecodingchallenge.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "course_table")
data class CourseModal(var courseName: String, var courseDescription: String,
    var courseDuration: String) {
    @PrimaryKey(autoGenerate = true)
    var id = 0

}
