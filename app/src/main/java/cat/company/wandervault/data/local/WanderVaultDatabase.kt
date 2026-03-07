package cat.company.wandervault.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TripEntity::class, DestinationEntity::class, TransportEntity::class, TransportLegEntity::class, HotelEntity::class], version = 11)
@TypeConverters(DateConverters::class)
abstract class WanderVaultDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun destinationDao(): DestinationDao
    abstract fun transportDao(): TransportDao
    abstract fun transportLegDao(): TransportLegDao
    abstract fun hotelDao(): HotelDao

    companion object {
        const val DATABASE_NAME = "wandervault.db"

        /** Flushes the WAL file into the main database file so it is safe to copy. */
        fun WanderVaultDatabase.checkpoint() {
            // wal_checkpoint returns result rows (busy, log, checkpointed), so it must be
            // executed via query rather than execSQL to avoid the "Queries can be performed
            // using SQLiteDatabase query or rawQuery methods only" error on Android.
            openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)")
                .use { it.moveToFirst() }
        }

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
                db.execSQL("PRAGMA foreign_keys=OFF")
                try {
                    // 1. Create the transport_legs table.
                    //    At DB version 7 each destination has at most one transport row, so each
                    //    existing transport row becomes a single leg with position = 0.
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `transport_legs` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `transportId` INTEGER NOT NULL,
                            `type` TEXT NOT NULL,
                            `position` INTEGER NOT NULL DEFAULT 0,
                            `company` TEXT,
                            `flightNumber` TEXT,
                            `reservationConfirmationNumber` TEXT,
                            FOREIGN KEY(`transportId`) REFERENCES `transports`(`id`)
                                ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_transport_legs_transportId` ON `transport_legs` (`transportId`)",
                    )

                    // 2. Migrate every existing transport row into transport_legs.
                    //    The transport row's own id becomes the transportId for the leg.
                    db.execSQL(
                        """
                        INSERT INTO `transport_legs` (`transportId`, `type`, `position`, `company`, `flightNumber`, `reservationConfirmationNumber`)
                        SELECT `id`, `type`, 0, `company`, `flightNumber`, `reservationConfirmationNumber`
                        FROM `transports`
                        """.trimIndent(),
                    )

                    // 3. Rebuild transports as a parent-only table (id, destinationId).
                    db.execSQL(
                        """
                        CREATE TABLE `transports_new` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `destinationId` INTEGER NOT NULL,
                            FOREIGN KEY(`destinationId`) REFERENCES `destinations`(`id`)
                                ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "INSERT INTO `transports_new` (`id`, `destinationId`) SELECT `id`, `destinationId` FROM `transports`",
                    )
                    db.execSQL("DROP TABLE `transports`")
                    db.execSQL("ALTER TABLE `transports_new` RENAME TO `transports`")
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_transports_destinationId` ON `transports` (`destinationId`)",
                    )

                    db.query("PRAGMA foreign_key_check").use { cursor ->
                        if (cursor.moveToFirst()) {
                            error("MIGRATION_7_8: foreign key integrity check failed")
                        }
                    }
                } finally {
                    db.execSQL("PRAGMA foreign_keys=ON")
                }
            }
        }
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transport_legs` ADD COLUMN `stopName` TEXT")
            }
        }
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `trips` ADD COLUMN `aiDescription` TEXT")
            }
        }
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `hotels` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `destinationId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL DEFAULT '',
                        `address` TEXT NOT NULL DEFAULT '',
                        `reservationNumber` TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(`destinationId`) REFERENCES `destinations`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_hotels_destinationId` ON `hotels` (`destinationId`)",
                )
            }
        }
    }
}
