package com.tradingsignal.app.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tradingsignal.app.data.model.Conditions

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromConditions(conditions: Conditions): String {
        return gson.toJson(conditions)
    }

    @TypeConverter
    fun toConditions(conditionsString: String): Conditions {
        val type = object : TypeToken<Conditions>() {}.type
        return gson.fromJson(conditionsString, type)
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return list?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toStringList(listString: String?): List<String>? {
        if (listString == null) return null
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(listString, type)
    }
}