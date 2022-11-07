package com.tsfapps.blecodingchallenge.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update


@Dao
interface Dao {
    @Insert
    fun insert(model: CourseModal?)

    @Update
    fun update(model: CourseModal?)

    @Delete
    fun delete(model: CourseModal?)

    @Query("DELETE FROM course_table")
    fun deleteAllCourses()

    @get:Query("SELECT * FROM course_table ORDER BY courseName ASC")
    val allCourses: LiveData<List<CourseModal>>
}
