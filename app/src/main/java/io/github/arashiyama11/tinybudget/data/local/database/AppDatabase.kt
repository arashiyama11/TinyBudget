package io.github.arashiyama11.tinybudget.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.arashiyama11.tinybudget.data.local.dao.CategoryDao
import io.github.arashiyama11.tinybudget.data.local.dao.TransactionDao
import io.github.arashiyama11.tinybudget.data.local.entity.Category
import io.github.arashiyama11.tinybudget.data.local.entity.Transaction

@Database(entities = [Category::class, Transaction::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // if the Instance is not null, return it, otherwise create a new database instance.
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { Instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN isEnabled INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}
