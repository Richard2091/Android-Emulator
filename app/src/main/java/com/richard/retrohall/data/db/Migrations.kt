package com.richard.retrohall.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room 数据库迁移：v2 目录模型引入 categoryId / platformId / runtimeFamily / detailUrl。
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE local_games ADD COLUMN categoryId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE local_games ADD COLUMN platformId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE local_games ADD COLUMN runtimeFamily TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE local_games ADD COLUMN detailUrl TEXT NOT NULL DEFAULT ''")
    }
}
