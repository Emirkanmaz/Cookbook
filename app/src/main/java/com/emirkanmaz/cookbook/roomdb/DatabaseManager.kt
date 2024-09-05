package com.emirkanmaz.cookbook.roomdb

import android.content.Context
import androidx.room.Room

object DatabaseManager {
    private var instance: RecipeDatabase? = null

    fun getInstance(context: Context): RecipeDatabase {
        if (instance == null) {
            instance = Room.databaseBuilder(
                context.applicationContext,
                RecipeDatabase::class.java,
                "Recipes"
            ).build()
        }
        return instance!!
    }
}