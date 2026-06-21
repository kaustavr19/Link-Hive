package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.Categorizer
import com.example.data.model.CategoryRule
import com.example.data.model.LinkRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [LinkRecord::class, CategoryRule::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun linkDao(): LinkDao
    abstract fun categoryRuleDao(): CategoryRuleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "linkhive_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default rules on creation
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                val defaultRules = Categorizer.getDefaultRules()
                                database.categoryRuleDao().insertRules(defaultRules)
                            }
                        }
                    }
                })
                .build()
                val tempInstance = INSTANCE ?: instance
                INSTANCE = tempInstance
                tempInstance
            }
        }
    }
}
