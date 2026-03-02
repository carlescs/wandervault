package cat.company.wandervault.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TripEntity::class], version = 2)
@TypeConverters(DateConverters::class)
abstract class WanderVaultDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao

    companion object {
        const val DATABASE_NAME = "wandervault.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trips ADD COLUMN imageUri TEXT")
            }
        }
    }
}
