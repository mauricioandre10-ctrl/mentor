package com.tecnovacenter.mentor.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tecnovacenter.mentor.data.Budget
import com.tecnovacenter.mentor.data.ConversationMessage
import com.tecnovacenter.mentor.data.Project

@Database(
    entities = [Project::class, ConversationMessage::class, Budget::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun conversationMessageDao(): ConversationMessageDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mentor_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
