package com.richard.retrohall.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [LocalGameEntity::class, SaveStateEntity::class],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class RetroHallDatabase : RoomDatabase() {
    abstract fun localGameDao(): LocalGameDao
    abstract fun saveStateDao(): SaveStateDao
}
