package com.tecnovacenter.mentor.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tecnovacenter.mentor.data.ConversationMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationMessageDao {

    @Query("SELECT * FROM conversation_messages WHERE projectId = :projectId ORDER BY timestamp ASC")
    fun getMessagesForProject(projectId: Long): Flow<List<ConversationMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ConversationMessage)

}
