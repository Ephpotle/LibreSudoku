package com.kaajjo.libresudoku.data.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey
import com.kaajjo.libresudoku.data.backup.serializer.DurationLongSerializer
import com.kaajjo.libresudoku.data.backup.serializer.ZonedDateTimeLongSerializer
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.ZonedDateTime

@Serializable
@Entity(
    tableName = "saved_game",
    foreignKeys = [ForeignKey(
        onDelete = CASCADE,
        entity = SudokuBoard::class,
        parentColumns = arrayOf("uid"),
        childColumns = arrayOf("board_uid")
    )]
)
data class SavedGame(
    @PrimaryKey
    @ColumnInfo(name = "board_uid") val uid: Long,
    @ColumnInfo(name = "current_board") val currentBoard: String,
    @ColumnInfo(name = "notes") val notes: String,
    @Serializable(with = DurationLongSerializer::class)
    @ColumnInfo(name = "timer") val timer: Duration,
    @ColumnInfo(name = "completed", defaultValue = "false") val completed: Boolean = false,
    @ColumnInfo(name = "give_up", defaultValue = "false") val giveUp: Boolean = false,
    @ColumnInfo(name = "mistakes", defaultValue = "0") val mistakes: Int = 0,
    @ColumnInfo(name = "can_continue") val canContinue: Boolean = true,
    @Serializable(with = ZonedDateTimeLongSerializer::class)
    @ColumnInfo(name = "last_played") val lastPlayed: ZonedDateTime? = null,
    @Serializable(with = ZonedDateTimeLongSerializer::class)
    @ColumnInfo(name = "started_at") val startedAt: ZonedDateTime? = null,
    @Serializable(with = ZonedDateTimeLongSerializer::class)
    @ColumnInfo(name = "finished_at") val finishedAt: ZonedDateTime? = null,
    // Aid use. NULL means unknown: the game was saved before these columns existed.
    @ColumnInfo(name = "hints_used") val hintsUsed: Int? = null,
    @ColumnInfo(name = "advanced_hints_used") val advancedHintsUsed: Int? = null,
    @ColumnInfo(name = "computed_notes_used") val computedNotesUsed: Int? = null,
    /** Highest mistake highlighting used during the game: 0 off, 1 rule conflicts, 2 compared with the solution. */
    @ColumnInfo(name = "mistake_marking") val mistakeMarking: Int? = null,
    @ColumnInfo(name = "try_again_used") val tryAgainUsed: Int? = null,
    /** How a finished game ended: see [GameEndReason]; NULL while playing or unknown. */
    @ColumnInfo(name = "end_reason") val endReason: String? = null,
    /** Build the game was first saved with, kept across app updates. */
    @ColumnInfo(name = "app_version_code") val appVersionCode: Int? = null,
    /** How hard the finished game felt to the player: see [FeltDifficulty]; NULL when not answered. */
    @ColumnInfo(name = "felt") val felt: String? = null,
    /**
     * Seconds of [timer] that came within [ACTIVE_WINDOW_MINUTES] minutes of the player's last input,
     * so time with the phone put down is left out. NULL when unknown: the game was started before this column existed.
     */
    @ColumnInfo(name = "active_seconds") val activeSeconds: Long? = null,
) {
    companion object {
        /** Minutes without any input after which the timer still runs but active time stops counting. */
        const val ACTIVE_WINDOW_MINUTES = 5
    }

    /** True when the game is known to have been played without any aid, false when one was used, null when unknown. */
    val unassisted: Boolean?
        get() {
            val counts = listOf(hintsUsed, advancedHintsUsed, computedNotesUsed, mistakeMarking)
            if (counts.any { it == null }) return null
            return hintsUsed == 0 && advancedHintsUsed == 0 && computedNotesUsed == 0 && mistakeMarking == 0
        }
}

/** Stored values of [SavedGame.endReason]. */
object GameEndReason {
    const val SOLVED = "solved"
    const val GAVE_UP = "gave_up"
    const val MISTAKE_LIMIT = "mistake_limit"
    /** Left unfinished when a new game was started. */
    const val ABANDONED = "abandoned"
}

/** Stored values of [SavedGame.felt], the player's own answer after a game. */
object FeltDifficulty {
    const val EASIER = "easier"
    const val RIGHT = "right"
    const val HARDER = "harder"
    val all = listOf(EASIER, RIGHT, HARDER)
}
