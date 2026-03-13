package con.open.tvos.data

import android.annotation.SuppressLint
import android.database.Cursor
import android.database.sqlite.SQLiteException
import androidx.annotation.NonNull
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import con.open.tvos.base.App
import con.open.tvos.util.FileUtils
import java.io.File
import java.io.IOException

/**
 * 类描述:
 *
 * @author pj567
 * @since 2020/5/15
 */
object AppDataManager {
    private const val DB_FILE_VERSION = 3
    private const val DB_NAME = "tvbox"
    
    private var manager: AppDataManager? = null
    private var dbInstance: AppDataBase? = null

    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                database.execSQL("ALTER TABLE sourceState ADD COLUMN tidSort TEXT")
            } catch (e: SQLiteException) {
                e.printStackTrace()
            }
        }
    }

    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        @SuppressLint("Range")
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `vodRecordTmp` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vodId` TEXT, `updateTime` INTEGER NOT NULL, `sourceKey` TEXT, `data` BLOB, `dataJson` TEXT, `testMigration` INTEGER NOT NULL)")

            // Read every thing from the former Expense table
            val cursor: Cursor = database.query("SELECT * FROM vodRecord")

            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndex("id"))
                val vodId = cursor.getInt(cursor.getColumnIndex("vodId"))
                val updateTime = cursor.getLong(cursor.getColumnIndex("updateTime"))
                val sourceKey = cursor.getString(cursor.getColumnIndex("sourceKey"))
                val dataJson = cursor.getString(cursor.getColumnIndex("dataJson"))
                database.execSQL(
                    "INSERT INTO vodRecordTmp (id, vodId, updateTime, sourceKey, dataJson, testMigration) VALUES" +
                            " ('$id', '$vodId', '$updateTime', '$sourceKey', '$dataJson',0 )"
                )
            }

            // Delete the former table
            database.execSQL("DROP TABLE vodRecord")
            // Rename the current table to the former table name so that all other code continues to work
            database.execSQL("ALTER TABLE vodRecordTmp RENAME TO vodRecord")
        }
    }

    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                database.execSQL("ALTER TABLE vodRecord ADD COLUMN dataJson TEXT")
            } catch (e: SQLiteException) {
                e.printStackTrace()
            }
        }
    }

    val MIGRATION_4_5: Migration = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                database.execSQL("ALTER TABLE localSource ADD COLUMN type INTEGER NOT NULL DEFAULT 0")
            } catch (e: SQLiteException) {
                e.printStackTrace()
            }
        }
    }

    internal fun dbPath(): String {
        return "$DB_NAME.v$DB_FILE_VERSION.db"
    }

    fun init() {
        if (manager == null) {
            synchronized(AppDataManager::class.java) {
                if (manager == null) {
                    manager = AppDataManager()
                }
            }
        }
    }

    fun get(): AppDataBase {
        if (manager == null) {
            throw RuntimeException("AppDataManager is no init")
        }
        if (dbInstance == null) {
            dbInstance = Room.databaseBuilder(App.getInstance(), AppDataBase::class.java, dbPath())
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                //.addMigrations(MIGRATION_1_2)
                //.addMigrations(MIGRATION_2_3)
                //.addMigrations(MIGRATION_3_4)
                //.addMigrations(MIGRATION_4_5)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(@NonNull db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // LOG.i("数据库第一次创建成功")
                    }

                    override fun onOpen(@NonNull db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // LOG.i("数据库打开成功")
                    }
                })
                .allowMainThreadQueries() //可以在主线程操作
                .build()
        }
        return dbInstance!!
    }

    @Throws(IOException::class)
    fun backup(path: File): Boolean {
        if (dbInstance != null && dbInstance!!.isOpen) {
            dbInstance!!.close()
        }
        val db = App.getInstance().getDatabasePath(dbPath())
        return if (db.exists()) {
            FileUtils.copyFile(db, path)
            true
        } else {
            false
        }
    }

    @Throws(IOException::class)
    fun restore(path: File): Boolean {
        if (dbInstance != null && dbInstance!!.isOpen) {
            dbInstance!!.close()
        }
        val db = App.getInstance().getDatabasePath(dbPath())
        if (db.exists()) {
            db.delete()
        }
        if (db.parentFile?.exists() != true) {
            db.parentFile?.mkdirs()
        }
        FileUtils.copyFile(path, db)
        return true
    }
}
