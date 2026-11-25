package razek.fuchoquiniela.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import razek.fuchoquiniela.data.BracketAggregate
import razek.fuchoquiniela.data.BracketLeg
import razek.fuchoquiniela.data.BracketTeam
import razek.fuchoquiniela.data.BracketTie
import razek.fuchoquiniela.data.FuchoApi
import razek.fuchoquiniela.data.LiguillaResponse
import java.time.Instant

class LiguillaBracketTest {
    private val ame = BracketTeam(2, "América", "AME", seed = 1)
    private val mty = BracketTeam(45, "Monterrey", "MTY", seed = 8)
    private fun leg(date: String, played: Boolean = false, live: Boolean = false, high: Int? = null, low: Int? = null) =
        BracketLeg(1, date, played = played, isLive = live, high = high, low = low)

    private val first = "2026-11-26T03:10:00Z"
    private val second = "2026-11-29T03:10:00Z"

    @Test
    fun decodesTheBracket() {
        val text = """
            {"season":{"id":2,"name":"Apertura 2026","league":"Liga MX"},"visible":true,"projected":false,
             "regular_remaining":0,"champion":null,
             "stages":[{"round":"Cuartos de Final","name":"Cuartos de final","ties":[
               {"high":{"id":2,"name":"América","shortname":"AME","image_url":"https://x/ame.png","seed":1},
                "low":{"id":45,"name":"Monterrey","shortname":"MTY","image_url":null,"seed":8},
                "status":"pending","projected":false,"aggregate":null,"winner_id":null,"decided_by":null,
                "legs":[{"match_id":1881,"date":"$first","host_team_id":45,"played":false,"is_live":false,"high":null,"low":null},
                        {"match_id":1885,"date":"$second","host_team_id":2,"played":false,"is_live":false,"high":null,"low":null}]}]},
               {"round":"Final","name":"Final","ties":[{"high":null,"low":null,"status":"pending","projected":false,
                "aggregate":null,"winner_id":null,"decided_by":null,"legs":[]}]}]}
        """.trimIndent()
        val liguilla = FuchoApi.json.decodeFromString(LiguillaResponse.serializer(), text)
        assertTrue(liguilla.visible)
        assertNull(liguilla.champion)
        val tie = liguilla.stages[0].ties[0]
        assertEquals(1, tie.high?.seed)
        assertEquals(2, tie.legs[1].hostTeamId)
        assertEquals(TieNote.Legs(Instant.parse(first), Instant.parse(second)), tieNote(tie))
        assertEquals(TieNote.None, tieNote(liguilla.stages[1].ties[0]))
    }

    @Test
    fun picksTheLineUnderEachTie() {
        val legs = listOf(leg(first, played = true, high = 1, low = 1), leg(second, played = true, high = 0, low = 0))
        val finished = BracketTie(ame, mty, "finished", aggregate = BracketAggregate(1, 1), winnerId = 2, legs = legs)
        assertEquals(TieNote.ByPosition(1, 1), tieNote(finished.copy(decidedBy = "position")))
        assertEquals(TieNote.Aggregate(1, 1), tieNote(finished.copy(decidedBy = "aggregate")))
        assertEquals(TieNote.WonOnPenalties(1, 1), tieNote(finished.copy(decidedBy = "penalties")))
        assertEquals(TieNote.ToPenalties, tieNote(finished.copy(decidedBy = "penalties", winnerId = null)))

        val live = BracketTie(ame, mty, "in_progress", aggregate = BracketAggregate(1, 0),
            legs = listOf(leg(first, played = true, high = 1, low = 0), leg(second, live = true, high = 0, low = 0)))
        assertEquals(TieNote.Live, tieNote(live))

        assertEquals(TieNote.Projected, tieNote(BracketTie(ame, mty, projected = true)))
        assertEquals(TieNote.DatesToConfirm, tieNote(BracketTie(ame, mty)))
        assertEquals(TieNote.FirstLeg(Instant.parse(first)), tieNote(BracketTie(ame, mty, legs = listOf(leg(first)))))
        assertEquals(TieNote.SecondLegToSchedule, tieNote(BracketTie(ame, mty, legs = listOf(leg(first, played = true, high = 2, low = 0)))))
    }
}
