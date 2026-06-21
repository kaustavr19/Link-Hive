package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CategoryRule
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryRuleDao {
    @Query("SELECT * FROM category_rules ORDER BY domain ASC")
    fun getAllRulesFlow(): Flow<List<CategoryRule>>

    @Query("SELECT * FROM category_rules")
    suspend fun getAllRules(): List<CategoryRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CategoryRule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<CategoryRule>)

    @Query("DELETE FROM category_rules WHERE domain = :domain")
    suspend fun deleteRuleByDomain(domain: String)
}
