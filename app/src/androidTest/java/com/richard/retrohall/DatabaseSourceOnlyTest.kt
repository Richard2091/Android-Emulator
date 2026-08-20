package com.richard.retrohall

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.richard.retrohall.data.db.RetroHallDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 验证 App 数据库里不包含私有资源注入的游戏（如魂斗罗），只保留数据源同步内容。
 * 仅在已安装 App 且完成同步后运行。
 */
@RunWith(AndroidJUnit4::class)
class DatabaseSourceOnlyTest {
    @Test
    fun gameLibraryContainsOnlyDataSourceGames() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.databaseBuilder(context, RetroHallDatabase::class.java, "retrohall.db")
            .allowMainThreadQueries()
            .build()
        val games = db.localGameDao().getAll()
        val ids = games.map { it.id }
        println("[DatabaseTest] count=${games.size}")
        println("[DatabaseTest] contra=${games.filter { it.id.contains("contra", ignoreCase = true) }.map { it.id }}")

        assertTrue("游戏库应非空", games.isNotEmpty())
        assertFalse("不应包含私有资源魂斗罗", games.any { it.id.contains("contra", ignoreCase = true) })
        assertTrue("应包含数据源游戏", ids.contains("fc-0001"))
        db.close()
    }
}
