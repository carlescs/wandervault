package cat.company.wandervault.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TripEntity::class, DestinationEntity::class], version = 5)
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS trips_new")
                db.execSQL(
                    "CREATE TABLE trips_new (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `imageUri` TEXT)",
                )
                db.execSQL("INSERT INTO trips_new (id, title, imageUri) SELECT id, title, imageUri FROM trips")
                db.execSQL("DROP TABLE trips")
                db.execSQL("ALTER TABLE trips_new RENAME TO trips")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE destinations ADD COLUMN transport TEXT")
            }
        }
    }
}
