package com.tcc.face.utils

import android.content.Context
import android.content.SharedPreferences
import com.tcc.face.base.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesManager @Inject constructor(@ApplicationContext context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "my_shared_prefs"
    }

    // Function to save a string
    fun saveString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    // Function to retrieve a string
    fun getString(key: String, defaultValue: String = ""): String? {
        return sharedPreferences.getString(key, defaultValue)
    }



    // Function to save a boolean
    fun firstTime( value: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.First, value).apply()
    }

    // Function to retrieve a boolean
    fun getFirstTime( ): Boolean {
        return sharedPreferences.getBoolean(Constants.First, true)
    }

    // Function to save an integer
    fun saveInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    // Function to retrieve an integer
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    // Function to save a float
    fun saveFloat(key: String, value: Float) {
        sharedPreferences.edit().putFloat(key, value).apply()
    }

    // Function to retrieve a float
    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return sharedPreferences.getFloat(key, defaultValue)
    }

    // Function to save a long
    fun saveLong(key: String, value: Long) {
        sharedPreferences.edit().putLong(key, value).apply()
    }

    // Function to retrieve a long
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }

    // Function to clear a specific value
    fun removeValue(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    // Function to clear all data from SharedPreferences
    fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}
