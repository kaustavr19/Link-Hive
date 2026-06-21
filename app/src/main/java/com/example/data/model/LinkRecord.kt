package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "links")
data class LinkRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val url: String,
    val name: String,
    val category: String, // jobs | socials | videos | articles | uncategorized
    val source: String,
    val status: String = "not_applied", // jobs only: applied | not_applied | rejected
    val readState: String = "unread", // unread | reading | done
    val pinned: Boolean = false,
    val tags: String = "", // Comma-separated list of tags
    val summary: String? = null,
    val thumbnail: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val searchBlob: String
)
