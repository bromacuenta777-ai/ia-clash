package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.Top1TacticalBrain
import com.example.model.AggressionProfile
import com.example.model.ArenaCalibration
import com.example.model.BattlePhase
import com.example.model.DeckArchetype
import com.example.model.LaneThreat
import com.example.model.ThreatSide
import com.example.model.VisionFrameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RoyaleAI", appName)
    }

    @Test
    fun `tactical brain places 4-3 defense against heavy left threat`() {
        val brain = Top1TacticalBrain()
        val vision = VisionFrameState(
            elixir = 5.0f,
            readySlots = listOf(true, true, true, true),
            battlePhase = BattlePhase.IN_BATTLE,
            laneThreat = LaneThreat(
                leftThreat = 0.72f,
                rightThreat = 0.1f,
                dominantSide = ThreatSide.LEFT
            )
        )
        val move = brain.evaluateNextMove(
            vision = vision,
            deck = DeckArchetype.HOG_26_CYCLE,
            aggression = AggressionProfile.BALANCED_PRO,
            calibration = ArenaCalibration(),
            geminiReport = null,
            nowMs = 10_000L
        )
        assertNotNull(move)
        assertEquals(20, move?.row)
        assertEquals(9, move?.col)
    }

    @Test
    fun `tactical brain prevents elixir leak at 9_5 elixir`() {
        val brain = Top1TacticalBrain()
        val vision = VisionFrameState(
            elixir = 9.5f,
            readySlots = listOf(true, true, true, true),
            battlePhase = BattlePhase.IN_BATTLE,
            laneThreat = LaneThreat()
        )
        val move = brain.evaluateNextMove(
            vision = vision,
            deck = DeckArchetype.HOG_26_CYCLE,
            aggression = AggressionProfile.DEFENSIVE_COUNTER,
            calibration = ArenaCalibration(),
            geminiReport = null,
            nowMs = 20_000L
        )
        assertNotNull(move)
        assertTrue((move?.row ?: 0) >= 16)
    }
}
