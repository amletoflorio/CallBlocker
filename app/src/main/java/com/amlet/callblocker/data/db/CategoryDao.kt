package com.amlet.callblocker.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) for the [CategoryEntity] table.
 *
 * All write operations are suspend functions so they must be called from a coroutine.
 * [getAllCategories] returns a reactive [Flow] that recomposes the UI automatically
 * whenever the underlying data changes.
 */
@Dao
interface CategoryDao {

    /** Returns a live stream of all categories ordered alphabetically. */
    @Query("SELECT * FROM contact_categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    /** One-shot snapshot of all categories — used for non-reactive contexts. */
    @Query("SELECT * FROM contact_categories ORDER BY name ASC")
    suspend fun getAllCategoriesSnapshot(): List<CategoryEntity>

    /** Looks up a category by its primary key. Returns null if not found. */
    @Query("SELECT * FROM contact_categories WHERE id = :id")
    suspend fun findById(id: Int): CategoryEntity?

    /** Inserts a new category. Replaces on conflict (same primary key). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    /** Updates an existing category record. */
    @Update
    suspend fun update(category: CategoryEntity)

    /** Deletes a category. Contacts that referenced it will have categoryId set to null. */
    @Delete
    suspend fun delete(category: CategoryEntity)

    /** Bulk-insert used when restoring from backup. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    /** Removes all categories — called before a full import. */
    @Query("DELETE FROM contact_categories")
    suspend fun deleteAll()
}
