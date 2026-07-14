package com.example.gittracker.data.local

import androidx.room.TypeConverter
import com.example.gittracker.data.model.ReleaseAsset
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromAssetsList(value: List<ReleaseAsset>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toAssetsList(value: String): List<ReleaseAsset> {
        val listType = object : TypeToken<List<ReleaseAsset>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
}
