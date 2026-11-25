package com.kaajjo.libresudoku.data

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kaajjo.libresudoku.data.database.AppDatabase
import com.kaajjo.libresudoku.data.database.model.FeltDifficulty
import com.kaajjo.libresudoku.data.database.model.GameEndReason
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Opens a database written by the version-7 schema (the one on players' phones before aid tracking)
 * with the current [AppDatabase] and its migrations, so Room's own schema check runs on the result.
 * The version-7 tables are created from the exported schema file, not from current code.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SavedGameMigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private var database: AppDatabase? = null

    @Before
    fun createVersion7Database() {
        context.deleteDatabase(DATABASE_NAME)
        val schema = JSONObject(schemaFile(7).readText()).getJSONObject("database")
        val db = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(DATABASE_NAME).apply { parentFile?.mkdirs() }, null)
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            val indices = entity.optJSONArray("indices") ?: continue
            for (j in 0 until indices.length()) {
                db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
        }
        val setup = schema.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) db.execSQL(setup.getString(i))

        db.execSQL(
            "INSERT INTO board (uid, initial_board, solved_board, difficulty, type) VALUES (7, '000', '123', 4, 0)"
        )
        db.execSQL(
            "INSERT INTO saved_game (board_uid, current_board, notes, timer, completed, give_up, mistakes, can_continue) " +
                "VALUES (7, '120', '', 30, 1, 0, 2, 0)"
        )
        db.version = 7
        db.close()
    }

    @After
    fun closeDatabase() {
        database?.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun `a version 7 database opens with the current schema and keeps its games`() = runBlocking {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
            .addMigrations(AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9)
            .allowMainThreadQueries()
            .build()
            .also { database = it }

        // Room validates every table against the current entities when the migrated database opens.
        val old = db.savedGameDao().get(7)
        assertNotNull(old)
        old!!
        assertEquals("120", old.currentBoard)
        assertEquals(2, old.mistakes)
        assertTrue(old.completed)
        assertFalse(old.canContinue)
        // Games saved before aid tracking stay unknown, never "unassisted".
        assertNull(old.hintsUsed)
        assertNull(old.unassisted)
        assertNull(old.endReason)
        assertNull(old.appVersionCode)
        assertNull(old.felt)

        db.savedGameDao().update(old.copy(hintsUsed = 1, endReason = GameEndReason.GAVE_UP, felt = FeltDifficulty.HARDER))
        val updated = db.savedGameDao().get(7)!!
        assertEquals(1, updated.hintsUsed)
        assertEquals(GameEndReason.GAVE_UP, updated.endReason)
        assertEquals(FeltDifficulty.HARDER, updated.felt)
        assertEquals(30L, updated.timer.seconds)
    }

    private fun schemaFile(version: Int): File {
        val relative = "schemas/com.kaajjo.libresudoku.data.database.AppDatabase/$version.json"
        return listOf(File(relative), File("app/$relative")).first { it.exists() }
    }

    private companion object {
        const val DATABASE_NAME = "migration_test_database"
    }
}
