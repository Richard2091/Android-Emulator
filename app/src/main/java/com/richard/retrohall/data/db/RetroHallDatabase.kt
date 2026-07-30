package com.richard.retrohall.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LocalGameEntity::class, SaveStateEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class RetroHallDatabase : RoomDatabase() {
    abstract fun localGameDao(): LocalGameDao
    abstract fun saveStateDao(): SaveStateDao
}
