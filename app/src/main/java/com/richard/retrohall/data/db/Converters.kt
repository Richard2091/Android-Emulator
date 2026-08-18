package com.richard.retrohall.data.db

import androidx.room.TypeConverter
import org.json.JSONArray

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return JSONArray(value).toString()
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isBlank()) {
            emptyList()
        } else {
            val array = JSONArray(value)
            buildList {
                for (index in 0 until array.length()) {
                    add(array.optString(index))
                }
            }
        }
    }
}
