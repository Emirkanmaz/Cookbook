package com.emirkanmaz.cookbook.roomdb

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.emirkanmaz.cookbook.model.Recipe
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable

@Dao
interface RecipeDAO {

    @Query("SELECT * FROM Recipe")
    fun getall(): Flowable<List<Recipe>>

    @Query("SELECT * FROM Recipe WHERE id = :id")
    fun findById(id: Int): Flowable<Recipe>

    @Insert
    fun insertAll(recipe: Recipe): Completable

    @Delete
    fun deleteAll(recipe: Recipe): Completable

    @Query("UPDATE Recipe SET name = :name, ingredient = :ingredient, image = :image WHERE id = :id")
    fun updateById(id: Int, name:String, ingredient: String, image: ByteArray? = null) : Completable

}