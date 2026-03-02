package cat.company.wandervault.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TripEntity::class, DestinationEntity::class], version = 3)
@TypeConverters(DateConverters::class)
abstract class WanderVaultDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun destinationDao(): DestinationDao

    companion object {
        const val DATABASE_NAME = "wandervault.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trips ADD COLUMN imageUri TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS destinations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tripId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        arrivalDateTime TEXT,
                        departureDateTime TEXT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_destinations_tripId_position ON destinations(tripId, position)",
                )
            }
        }
    }
}
