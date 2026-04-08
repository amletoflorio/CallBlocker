package com.amlet.callblocker.data.repository

import com.amlet.callblocker.data.db.CategoryDao
import com.amlet.callblocker.data.db.CategoryEntity
import com.amlet.callblocker.data.db.ContactDao
import kotlinx.coroutines.flow.Flow

/**
 * Repository for [CategoryEntity] data.
 *
 * Provides a single source of truth for category CRUD operations and
 * handles the side-effect of clearing [categoryId] on orphaned contacts
 * whenever a category is deleted.
 */
class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val contactDao: ContactDao
) {

    /** Reactive stream of all categories, ordered alphabetically. */
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    /** Inserts a new category and returns its generated ID. */
    suspend fun addCategory(name: String, emoji: String = ""): Long =
        categoryDao.insert(CategoryEntity(name = name.trim(), emoji = emoji.trim()))

    /** Updates an existing category's name and/or emoji. */
    suspend fun updateCategory(category: CategoryEntity) =
        categoryDao.update(category)

    /**
     * Deletes a category and clears the [categoryId] reference on any contacts
     * that were assigned to it, so no orphan references remain.
     */
    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.delete(category)
        // Nullify categoryId on contacts that referenced this category.
        val contacts = contactDao.getAllContacts() // non-reactive snapshot via extension
        // We operate on a one-shot list obtained through the flow first emission.
        // The actual null-out is done via an explicit DAO query below.
        contactDao.clearCategoryFromContacts(category.id)
    }

    /** Returns all categories as a one-shot list (non-reactive). */
    suspend fun getSnapshot(): List<CategoryEntity> = categoryDao.getAllCategoriesSnapshot()
}
