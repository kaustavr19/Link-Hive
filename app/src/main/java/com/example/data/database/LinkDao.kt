package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.LinkRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkDao {
    @Query("SELECT * FROM links ORDER BY createdAt DESC")
    fun getAllLinksFlow(): Flow<List<LinkRecord>>

    @Query("SELECT * FROM links WHERE id = :id LIMIT 1")
    suspend fun getLinkById(id: String): LinkRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: LinkRecord)

    @Update
    suspend fun updateLink(link: LinkRecord)

    @Query("DELETE FROM links WHERE id = :id")
    suspend fun deleteLinkById(id: String)

    @Delete
    suspend fun deleteLink(link: LinkRecord)
}
