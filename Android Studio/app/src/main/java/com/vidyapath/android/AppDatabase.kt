package com.vidyapath.android

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey val accountId: String,
    val name: String,
    val email: String,
    val onboarded: Boolean,
    val subjectsJson: String,
    val completedTopicsJson: String,
    val examSubject: String,
    val examDate: String,
    val examPlanJson: String,
    val notifications: Boolean,
    val darkMode: Boolean,
    val language: String,
    val avatarFileName: String?,
)

@Entity(tableName = "study_tasks")
data class StudyTaskEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val subject: String,
    val topic: String,
    val date: String,
    val minutes: Int,
    val priority: String,
    val done: Boolean,
    val completedAt: Long?,
)

@Entity(tableName = "study_reminders")
data class StudyReminderEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val subject: String,
    val topic: String,
    val date: String,
    val intervalDays: Int,
    val done: Boolean,
)

@Entity(tableName = "study_resources")
data class StudyResourceEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val type: String,
    val title: String,
    val url: String,
    val subject: String,
    val topic: String,
    val fileName: String?,
    val originalFileName: String?,
)

@Dao
abstract class AppDao {
    @Query("SELECT * FROM user_accounts WHERE accountId = :accountId LIMIT 1")
    abstract suspend fun getUserAccount(accountId: String): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUserAccount(account: UserAccountEntity)

    @Query("SELECT * FROM study_tasks WHERE accountId = :accountId")
    abstract suspend fun getTasks(accountId: String): List<StudyTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTasks(tasks: List<StudyTaskEntity>)

    @Query("DELETE FROM study_tasks WHERE accountId = :accountId")
    abstract suspend fun deleteTasksForAccount(accountId: String)

    @Query("SELECT * FROM study_reminders WHERE accountId = :accountId")
    abstract suspend fun getReminders(accountId: String): List<StudyReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertReminders(reminders: List<StudyReminderEntity>)

    @Query("DELETE FROM study_reminders WHERE accountId = :accountId")
    abstract suspend fun deleteRemindersForAccount(accountId: String)

    @Query("SELECT * FROM study_resources WHERE accountId = :accountId")
    abstract suspend fun getResources(accountId: String): List<StudyResourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertResources(resources: List<StudyResourceEntity>)

    @Query("DELETE FROM study_resources WHERE accountId = :accountId")
    abstract suspend fun deleteResourcesForAccount(accountId: String)

    @Transaction
    open suspend fun saveFullAppData(
        account: UserAccountEntity,
        tasks: List<StudyTaskEntity>,
        reminders: List<StudyReminderEntity>,
        resources: List<StudyResourceEntity>,
    ) {
        insertUserAccount(account)
        deleteTasksForAccount(account.accountId)
        if (tasks.isNotEmpty()) insertTasks(tasks)
        deleteRemindersForAccount(account.accountId)
        if (reminders.isNotEmpty()) insertReminders(reminders)
        deleteResourcesForAccount(account.accountId)
        if (resources.isNotEmpty()) insertResources(resources)
    }
}

@Database(
    entities = [
        UserAccountEntity::class,
        StudyTaskEntity::class,
        StudyReminderEntity::class,
        StudyResourceEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vidyapath_database",
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
