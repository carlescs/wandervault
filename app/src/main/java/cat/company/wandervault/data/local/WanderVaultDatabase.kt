package cat.company.wandervault.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [TripEntity::class], version = 1)
@TypeConverters(DateConverters::class)
abstract class WanderVaultDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao

    companion object {
        const val DATABASE_NAME = "wandervault.db"
    }
}
