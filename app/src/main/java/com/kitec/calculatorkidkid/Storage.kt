package com.kitec.calculatorkidkid

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.*
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.math.RoundingMode

@Entity(tableName = "history")
data class HistoryEntry(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expression: String, val value: String, val created: Long = System.currentTimeMillis())
@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY created DESC, id DESC") fun all(): List<HistoryEntry>
    @Insert(onConflict = OnConflictStrategy.ABORT) fun insert(entry: HistoryEntry): Long
    @Query("DELETE FROM history WHERE id = :id") fun delete(id: Long)
    @Query("DELETE FROM history") fun clear()
    @Query("DELETE FROM history WHERE created < :cutoff") fun expire(cutoff: Long)
    @Query("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY created DESC, id DESC LIMIT :maximum)") fun cap(maximum: Int)
    @Transaction fun prune(days: Int, maximum: Int) { expire(retentionCutoff(days)); cap(maximum) }
}
fun retentionCutoff(days: Int, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Long =
    today.minusDays(days.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
@Database(entities = [HistoryEntry::class], version = 1, exportSchema = true)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun history(): HistoryDao
    companion object {
        fun open(context: Context): HistoryDatabase = Room.databaseBuilder(context.applicationContext,
            HistoryDatabase::class.java, "history.db").openHelperFactory { config ->
                val original = config.callback
                val callback = object : SupportSQLiteOpenHelper.Callback(original.version) {
                    override fun onConfigure(db: SupportSQLiteDatabase) = original.onConfigure(db)
                    override fun onCreate(db: SupportSQLiteDatabase) = original.onCreate(db)
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = original.onUpgrade(db, oldVersion, newVersion)
                    override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = original.onDowngrade(db, oldVersion, newVersion)
                    override fun onOpen(db: SupportSQLiteDatabase) = original.onOpen(db)
                    override fun onCorruption(db: SupportSQLiteDatabase) {
                        throw android.database.sqlite.SQLiteDatabaseCorruptException("History unavailable; original database preserved")
                    }
                }
                FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(config.context)
                    .name(config.name).callback(callback).build())
            }.build()
        // No destructive migration or corruption recovery. Version changes require
        // explicit migrations; Room's default callback may delete corrupt files,
        // so the application supplies a non-destructive open helper below.
    }
}

data class Settings(val language: String = "th", val theme: String = "system", val places: Int = 10,
    val rounding: RoundingMode = RoundingMode.HALF_UP, val days: Int = 30, val maximum: Int = 300)
class Preferences(context: Context) {
    private val prefs = context.getSharedPreferences("calculator", Context.MODE_PRIVATE)
    fun settings() = Settings(prefs.getString("language", "th") ?: "th", prefs.getString("theme", "system") ?: "system",
        prefs.getInt("places", 10).coerceIn(0,20),
        runCatching { RoundingMode.valueOf(prefs.getString("rounding", "HALF_UP")!!) }.getOrDefault(RoundingMode.HALF_UP),
        prefs.getInt("days",30), prefs.getInt("maximum",300))
    fun save(s: Settings): Boolean = prefs.edit().putString("language",s.language).putString("theme",s.theme)
        .putInt("places",s.places).putString("rounding",s.rounding.name).putInt("days",s.days).putInt("maximum",s.maximum).commit()
    fun memory(index: Int): String? = prefs.getString("m$index",null)
    fun memory(index: Int, value: String?): Boolean = prefs.edit().putString("m$index",value).commit()
}
