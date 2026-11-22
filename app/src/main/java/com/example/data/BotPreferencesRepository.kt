package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AggressionProfile
import com.example.model.ArenaCalibration
import com.example.model.DeckArchetype
import com.example.model.GestureMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.botDataStore: DataStore<Preferences> by preferencesDataStore(name = "royale_ai_prefs")

data class StoredBotConfig(
    val deckArchetype: DeckArchetype = DeckArchetype.HOG_26_CYCLE,
    val gestureMode: GestureMode = GestureMode.TAP_THEN_PLACE,
    val aggressionProfile: AggressionProfile = AggressionProfile.BALANCED_PRO,
    val calibration: ArenaCalibration = ArenaCalibration(),
    val totalGeminiScans: Int = 0
)

class BotPreferencesRepository(private val context: Context) {

    private object Keys {
        val DECK = stringPreferencesKey("deck_archetype")
        val GESTURE_MODE = stringPreferencesKey("gesture_mode")
        val AGGRESSION = stringPreferencesKey("aggression_profile")
        val ARENA_TOP = floatPreferencesKey("arena_top")
        val ARENA_BOTTOM = floatPreferencesKey("arena_bottom")
        val ARENA_LEFT = floatPreferencesKey("arena_left")
        val ARENA_RIGHT = floatPreferencesKey("arena_right")
        val ELIXIR_Y = floatPreferencesKey("elixir_y")
        val CARDS_Y = floatPreferencesKey("cards_y")
        val GESTURE_SPEED_MS = longPreferencesKey("gesture_speed_ms")
        val HUMAN_JITTER_PX = intPreferencesKey("human_jitter_px")
        val AUTO_QUEUE = booleanPreferencesKey("auto_queue")
        val GEMINI_LOOP = booleanPreferencesKey("gemini_loop")
        val GEMINI_SCANS = intPreferencesKey("gemini_scans")
    }

    val configFlow: Flow<StoredBotConfig> = context.botDataStore.data.map { prefs ->
        val deck = prefs[Keys.DECK]?.let { name ->
            DeckArchetype.entries.firstOrNull { it.name == name }
        } ?: DeckArchetype.HOG_26_CYCLE

        val gesture = prefs[Keys.GESTURE_MODE]?.let { name ->
            GestureMode.entries.firstOrNull { it.name == name }
        } ?: GestureMode.TAP_THEN_PLACE

        val aggression = prefs[Keys.AGGRESSION]?.let { name ->
            AggressionProfile.entries.firstOrNull { it.name == name }
        } ?: AggressionProfile.BALANCED_PRO

        val defaultCal = ArenaCalibration()
        val calibration = defaultCal.copy(
            arenaTop = prefs[Keys.ARENA_TOP] ?: defaultCal.arenaTop,
            arenaBottom = prefs[Keys.ARENA_BOTTOM] ?: defaultCal.arenaBottom,
            arenaLeft = prefs[Keys.ARENA_LEFT] ?: defaultCal.arenaLeft,
            arenaRight = prefs[Keys.ARENA_RIGHT] ?: defaultCal.arenaRight,
            elixirBarY = prefs[Keys.ELIXIR_Y] ?: defaultCal.elixirBarY,
            cardsY = prefs[Keys.CARDS_Y] ?: defaultCal.cardsY,
            gestureSpeedMs = prefs[Keys.GESTURE_SPEED_MS] ?: defaultCal.gestureSpeedMs,
            humanJitterPx = prefs[Keys.HUMAN_JITTER_PX] ?: defaultCal.humanJitterPx,
            autoQueueEnabled = prefs[Keys.AUTO_QUEUE] ?: defaultCal.autoQueueEnabled,
            useGeminiVisionLoop = prefs[Keys.GEMINI_LOOP] ?: defaultCal.useGeminiVisionLoop
        )

        StoredBotConfig(
            deckArchetype = deck,
            gestureMode = gesture,
            aggressionProfile = aggression,
            calibration = calibration,
            totalGeminiScans = prefs[Keys.GEMINI_SCANS] ?: 0
        )
    }

    suspend fun saveDeck(deck: DeckArchetype) {
        context.botDataStore.edit { it[Keys.DECK] = deck.name }
    }

    suspend fun saveGestureMode(mode: GestureMode) {
        context.botDataStore.edit { it[Keys.GESTURE_MODE] = mode.name }
    }

    suspend fun saveAggression(profile: AggressionProfile) {
        context.botDataStore.edit { it[Keys.AGGRESSION] = profile.name }
    }

    suspend fun saveCalibration(cal: ArenaCalibration) {
        context.botDataStore.edit {
            it[Keys.ARENA_TOP] = cal.arenaTop
            it[Keys.ARENA_BOTTOM] = cal.arenaBottom
            it[Keys.ARENA_LEFT] = cal.arenaLeft
            it[Keys.ARENA_RIGHT] = cal.arenaRight
            it[Keys.ELIXIR_Y] = cal.elixirBarY
            it[Keys.CARDS_Y] = cal.cardsY
            it[Keys.GESTURE_SPEED_MS] = cal.gestureSpeedMs
            it[Keys.HUMAN_JITTER_PX] = cal.humanJitterPx
            it[Keys.AUTO_QUEUE] = cal.autoQueueEnabled
            it[Keys.GEMINI_LOOP] = cal.useGeminiVisionLoop
        }
    }

    suspend fun incrementGeminiScans() {
        context.botDataStore.edit { prefs ->
            val current = prefs[Keys.GEMINI_SCANS] ?: 0
            prefs[Keys.GEMINI_SCANS] = current + 1
        }
    }
}
