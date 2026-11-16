package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.FocusSessionDao
import com.example.data.local.dao.SubjectDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.FocusSessionEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskEntity

@Database(
    entities = [
        SubjectEntity::class,
        TaskEntity::class,
        FocusSessionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SaviiFocusDatabase : RoomDatabase() {

    abstract fun subjectDao(): SubjectDao
    abstract fun taskDao(): TaskDao
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        private const val DATABASE_NAME = "savii_focus.db"

        /**
         * Preserves all existing v1 subjects, IDs, task links, and focus history while
         * adding Study category ('AL' default matching the prior flat subject list),
         * AAT level code (0 = N/A), icon key, and scoped uniqueness index.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE subjects ADD COLUMN category TEXT NOT NULL DEFAULT 'AL'")
                db.execSQL("ALTER TABLE subjects ADD COLUMN aatLevel INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE subjects ADD COLUMN iconKey TEXT NOT NULL DEFAULT 'book'")
                db.execSQL("DROP INDEX IF EXISTS index_subjects_normalizedName")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_subjects_normalizedName_category_aatLevel ON subjects (normalizedName, category, aatLevel)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_subjects_category_aatLevel ON subjects (category, aatLevel)"
                )
            }
        }

        private val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)

        @Volatile
        private var instance: SaviiFocusDatabase? = null

        fun getInstance(context: Context): SaviiFocusDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SaviiFocusDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(*MIGRATIONS)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
