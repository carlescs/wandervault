package cat.company.wandervault.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDateTime
import java.time.ZoneId

@Database(
    entities = [
        TripEntity::class,
        DestinationEntity::class,
        TransportEntity::class,
        TransportLegEntity::class,
        HotelEntity::class,
        TripDocumentFolderEntity::class,
        TripDocumentEntity::class,
        ActivityEntity::class,
    ],
    version = 24,
)
@TypeConverters(DateConverters::class)
abstract class WanderVaultDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun destinationDao(): DestinationDao
    abstract fun transportDao(): TransportDao
    abstract fun transportLegDao(): TransportLegDao
    abstract fun hotelDao(): HotelDao
    abstract fun tripDocumentFolderDao(): TripDocumentFolderDao
    abstract fun tripDocumentDao(): TripDocumentDao
    abstract fun activityDao(): ActivityDao

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
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `destinations` ADD COLUMN `notes` TEXT")
            }
        }
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `trips` ADD COLUMN `isFavorite` INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create trip_document_folders
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `trip_document_folders` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `tripId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `parentFolderId` INTEGER,
                        FOREIGN KEY(`tripId`) REFERENCES `trips`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`parentFolderId`) REFERENCES `trip_document_folders`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_trip_document_folders_tripId` ON `trip_document_folders` (`tripId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_trip_document_folders_parentFolderId` ON `trip_document_folders` (`parentFolderId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_trip_document_folders_tripId_parentFolderId_name` ON `trip_document_folders` (`tripId`, `parentFolderId`, `name`)")

                // Create trip_documents
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `trip_documents` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `folderId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `uri` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        FOREIGN KEY(`folderId`) REFERENCES `trip_document_folders`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_trip_documents_folderId` ON `trip_documents` (`folderId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_trip_documents_folderId_name` ON `trip_documents` (`folderId`, `name`)")
            }
        }
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Explicitly disable foreign key enforcement during this complex migration,
                // mirroring the pattern used in other migrations that rename, recreate, and drop tables.
                db.execSQL("PRAGMA foreign_keys=OFF")
                try {
                    // Recreate trip_documents to add non-null tripId column and make folderId nullable.
                    // SQLite does not support ALTER COLUMN, so we rename, recreate, copy, and drop.
                    db.execSQL("ALTER TABLE `trip_documents` RENAME TO `trip_documents_old`")
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `trip_documents` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `tripId` INTEGER NOT NULL,
                            `folderId` INTEGER,
                            `name` TEXT NOT NULL,
                            `uri` TEXT NOT NULL,
                            `mimeType` TEXT NOT NULL,
                            FOREIGN KEY(`tripId`) REFERENCES `trips`(`id`)
                                ON UPDATE NO ACTION ON DELETE CASCADE,
                            FOREIGN KEY(`folderId`) REFERENCES `trip_document_folders`(`id`)
                                ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    // Copy existing rows; derive tripId from the folder's tripId via a join.
                    db.execSQL(
                        """
                        INSERT INTO `trip_documents` (`id`, `tripId`, `folderId`, `name`, `uri`, `mimeType`)
                        SELECT d.`id`, f.`tripId`, d.`folderId`, d.`name`, d.`uri`, d.`mimeType`
                        FROM `trip_documents_old` d
                        JOIN `trip_document_folders` f ON f.`id` = d.`folderId`
                        """.trimIndent(),
                    )
                    db.execSQL("DROP TABLE `trip_documents_old`")

                    // Verify that the new schema does not violate any foreign key constraints.
                    // foreign_key_check returns columns: table, rowid, parent, fkid.
                    // We extract the first three to build an actionable error message.
                    db.query("PRAGMA foreign_key_check").use { cursor ->
                        if (cursor.moveToFirst()) {
                            val table = cursor.getString(0)
                            val rowId = cursor.getString(1)
                            val parent = cursor.getString(2)
                            error(
                                "MIGRATION_14_15: foreign key constraint violation — " +
                                    "row $rowId in '$table' has no matching record in '$parent'. " +
                                    "This indicates corrupt data before migration. " +
                                    "Consider clearing app data and restoring from backup.",
                            )
                        }
                    }

                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_trip_documents_tripId` ON `trip_documents` (`tripId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_trip_documents_folderId` ON `trip_documents` (`folderId`)")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_trip_documents_folderId_name` ON `trip_documents` (`folderId`, `name`)")
                } finally {
                    db.execSQL("PRAGMA foreign_keys=ON")
                }
            }
        }
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `trip_documents` ADD COLUMN `summary` TEXT")
            }
        }
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `transport_legs` ADD COLUMN `isDefault` INTEGER NOT NULL DEFAULT 0",
                )
            }
        }
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Adds per-leg departure/arrival date-times. Existing rows are left as NULL,
                // which means "inherit from the parent destination" at runtime (the ViewModel
                // reads destination.departureDateTime for the first leg and
                // nextDestination.arrivalDateTime for the last leg when the stored value is null).
                db.execSQL("ALTER TABLE `transport_legs` ADD COLUMN `departureDateTime` TEXT")
                db.execSQL("ALTER TABLE `transport_legs` ADD COLUMN `arrivalDateTime` TEXT")
            }
        }
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add per-trip default timezone column (nullable; null means device default).
                db.execSQL("ALTER TABLE `trips` ADD COLUMN `defaultTimezone` TEXT")

                // 2. Best-effort migration of existing LocalDateTime strings to ZonedDateTime
                //    format. Existing values are ISO-8601 local strings (no zone, no offset),
                //    e.g. "2024-06-01T12:00:00". We append the device's current default zone
                //    so they become valid ZonedDateTime strings on first read.
                val zoneId = ZoneId.systemDefault()

                // Helper: re-encode a single TEXT column in a table.
                fun migrateColumn(table: String, idCol: String, col: String) {
                    db.query("SELECT `$idCol`, `$col` FROM `$table` WHERE `$col` IS NOT NULL")
                        .use { cursor ->
                            while (cursor.moveToNext()) {
                                val rowId = cursor.getLong(0)
                                val raw = cursor.getString(1) ?: continue
                                // Skip values that already carry zone information (ZonedDateTime
                                // format always contains an offset '+'/'-' or 'Z', and an IANA
                                // zone ID in brackets).  Only bare LocalDateTime strings like
                                // "2024-06-01T12:00:00" need to be upgraded.
                                // Use a parse attempt as the authoritative check instead of
                                // simple character-matching to avoid false positives.
                                try {
                                    java.time.ZonedDateTime.parse(raw)
                                    continue  // already a ZonedDateTime – nothing to do
                                } catch (_: Exception) {
                                    // Not yet a ZonedDateTime; fall through to conversion below.
                                }
                                val converted = try {
                                    LocalDateTime.parse(raw).atZone(zoneId).toString()
                                } catch (_: Exception) {
                                    continue
                                }
                                db.execSQL(
                                    "UPDATE `$table` SET `$col` = ? WHERE `$idCol` = ?",
                                    arrayOf<Any>(converted, rowId),
                                )
                            }
                        }
                }

                migrateColumn("destinations", "id", "arrivalDateTime")
                migrateColumn("destinations", "id", "departureDateTime")
                migrateColumn("transport_legs", "id", "departureDateTime")
                migrateColumn("transport_legs", "id", "arrivalDateTime")
            }
        }
        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `trips` ADD COLUMN `nextStep` TEXT")
                db.execSQL("ALTER TABLE `trips` ADD COLUMN `nextStepDeadline` TEXT")
            }
        }
        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `trips` ADD COLUMN `isArchived` INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `activities` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `destinationId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL DEFAULT '',
                        `description` TEXT NOT NULL DEFAULT '',
                        `dateTime` TEXT,
                        `confirmationNumber` TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(`destinationId`) REFERENCES `destinations`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_activities_destinationId` ON `activities` (`destinationId`)",
                )
            }
        }
        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transport_legs` ADD COLUMN `sourceDocumentId` INTEGER")
                db.execSQL("ALTER TABLE `hotels` ADD COLUMN `sourceDocumentId` INTEGER")
                db.execSQL("ALTER TABLE `trips` ADD COLUMN `aiDescriptionSourceDocumentId` INTEGER")
            }
        }
        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `activities` ADD COLUMN `sourceDocumentId` INTEGER")
            }
        }
    }
}
