package io.github.arashiyama11.tinybudget.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.arashiyama11.tinybudget.data.local.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE isEnabled = 1 ORDER BY id ASC")
    fun getEnabled(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun findById(id: Int): Category?

    @Insert
    suspend fun insert(category: Category)

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)
}
