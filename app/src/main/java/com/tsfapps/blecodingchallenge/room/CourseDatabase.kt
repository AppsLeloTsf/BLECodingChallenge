package com.tsfapps.blecodingchallenge.room


import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CourseModal::class], version = 1)
abstract class CourseDatabase : RoomDatabase() {
    abstract fun Dao(): Dao
}
