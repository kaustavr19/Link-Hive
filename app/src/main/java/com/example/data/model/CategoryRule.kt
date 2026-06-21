package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_rules")
data class CategoryRule(
    @PrimaryKey val domain: String,
    val category: String
)
