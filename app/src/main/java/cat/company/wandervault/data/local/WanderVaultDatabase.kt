package cat.company.wandervault.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TripEntity::class, DestinationEntity::class, TransportEntity::class], version = 8)
@TypeConverters(DateConverters::class)
abstract class WanderVaultDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun destinationDao(): DestinationDao
    abstract fun transportDao(): TransportDao

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

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Disable FK enforcement for the duration of the migration so that dropping
                // and recreating the referenced `destinations` table does not violate the FK
                // constraint on `transports` (which we create in step 1).
                db.execSQL("PRAGMA foreign_keys=OFF")
                try {
                    // 1. Create the new transports table.
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS transports (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `destinationId` INTEGER NOT NULL,
                            `type` TEXT NOT NULL,
                            FOREIGN KEY(`destinationId`) REFERENCES `destinations`(`id`)
                                ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS index_transports_destinationId ON transports(destinationId)",
                    )
                    // 2. Migrate existing inline transport values into the new table.
                    db.execSQL(
                        "INSERT INTO transports (destinationId, type) SELECT id, transport FROM destinations WHERE transport IS NOT NULL",
                    )
                    // 3. Recreate destinations without the transport column.
                    //    SQLite does not support DROP COLUMN on Android API < 33.
                    db.execSQL(
                        """
                        CREATE TABLE destinations_new (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `tripId` INTEGER NOT NULL,
                            `name` TEXT NOT NULL,
                            `position` INTEGER NOT NULL,
                            `arrivalDateTime` TEXT,
                            `departureDateTime` TEXT
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "INSERT INTO destinations_new SELECT id, tripId, name, position, arrivalDateTime, departureDateTime FROM destinations",
                    )
                    db.execSQL("DROP TABLE destinations")
                    db.execSQL("ALTER TABLE destinations_new RENAME TO destinations")
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_destinations_tripId_position ON destinations(tripId, position)",
                    )
                    // Verify FK integrity before re-enabling enforcement.
                    // foreign_key_check returns one row per violation; throw if any are found.
                    db.query("PRAGMA foreign_key_check").use { cursor ->
                        if (cursor.moveToFirst()) {
                            error("MIGRATION_5_6: foreign key integrity check failed")
                        }
                    }
                } finally {
                    db.execSQL("PRAGMA foreign_keys=ON")
                }
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transports ADD COLUMN `company` TEXT")
                db.execSQL("ALTER TABLE transports ADD COLUMN `flightNumber` TEXT")
                db.execSQL("ALTER TABLE transports ADD COLUMN `reservationConfirmationNumber` TEXT")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add position column to allow multiple ordered legs per destination.
                db.execSQL("ALTER TABLE transports ADD COLUMN `position` INTEGER NOT NULL DEFAULT 0")
                // Remove the unique constraint on destinationId to allow multiple legs.
                db.execSQL("DROP INDEX IF EXISTS `index_transports_destinationId`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transports_destinationId` ON `transports`(`destinationId`)")
            }
        }
    }
}
