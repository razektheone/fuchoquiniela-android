package razek.fuchoquiniela.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ModelsTest {

    @Test
    fun parsesTheServerDateFormats() {
        val expected = Instant.parse("2026-11-22T01:05:00Z")
        assertEquals(expected, parseDate("2026-11-22T01:05:00Z"))
        assertEquals(expected, parseDate("2026-11-22T01:05:00.000000Z"))
        assertEquals(expected, parseDate("2026-11-21T19:05:00-06:00"))
        assertEquals(expected, parseDate("2026-11-22 01:05:00"))
        assertNull(parseDate("mañana"))
    }

    @Test
    fun readsPoolCodesFromInviteLinksAndBareCodes() {
        val uuid = "0f8fad5b-d9cb-469f-a165-70867728950e"
        assertEquals(uuid, poolUuid("https://www.fuchoquiniela.com/pool/$uuid/join"))
        assertEquals(uuid, poolUuid("  Únete a mi quiniela: fuchoquiniela.com/pool/$uuid/join "))
        assertEquals(uuid, poolUuid(uuid))
        assertNull(poolUuid("fuchoquiniela.com/pools"))
    }

    @Test
    fun decodesSnakeCasePayloads() {
        val text = """
            {"pools":[{"uuid":"u1","name":"Oficina","description":null,"price":"100.00","league":"Liga MX",
              "season":"Apertura 2026","season_id":7,"is_admin":true,"created_at":"2026-07-01T18:00:00Z",
              "participants_count":12,"unknown_key":1},
             {"uuid":"u2","name":"Familia","price":50,"season_id":7,"is_admin":false}]}
        """.trimIndent()
        val pools = FuchoApi.json.decodeFromString(PoolsResponse.serializer(), text).pools
        assertEquals(100.0, pools[0].priceValue, 0.0)
        assertEquals(50.0, pools[1].priceValue, 0.0)
        assertEquals(12, pools[0].participantsCount)
        assertEquals("Liga MX · Apertura 2026", pools[0].subtitle)
        assertEquals("", pools[1].subtitle)
        assertTrue(pools[0].isAdmin)

        val matches = FuchoApi.json.decodeFromString(
            MatchesResponse.serializer(),
            """{"server_time":"2026-11-17T12:00:00Z","matches":[{"id":1,"round_id":3,"round":"Jornada 17",
               "date":"2026-11-22T01:05:00Z","local":"América","visit":"Chivas","team_id_local":1,"team_id_visit":2,
               "score_local":null,"score_visit":null,"is_live":false,"locked":false,"pick":{"score_local":2,"score_visit":1}}]}""",
        ).matches
        assertEquals(PickScore(2, 1), matches[0].pick)
        assertEquals(Instant.parse("2026-11-22T01:05:00Z"), matches[0].kickoff)
    }

    @Test
    fun encodesPicksInSnakeCase() {
        val body = FuchoApi.json.encodeToString(SavePicksRequest.serializer(), SavePicksRequest(listOf(PickInput(9, 2, 0))))
        assertEquals("""{"picks":[{"match_id":9,"score_local":2,"score_visit":0}]}""", body)
    }

    private fun row(id: Int, position: Int, points: Int, gf: Int, ga: Int, name: String) = TableRow(
        position = position, team = TeamRef(id, name), goalDiff = gf - ga, played = 10, won = 0, drawn = 0, lost = 0,
        goalsFor = gf, goalsAgainst = ga, points = points,
    )

    @Test
    fun simulatorAddsScoresAndOrdersLikeTheWeb() {
        val teams = listOf(
            row(1, 1, 20, 15, 10, "América"),
            row(2, 2, 19, 14, 10, "Chivas"),
            row(3, 3, 19, 14, 10, "Atlas"),
        )
        val match = LeagueMatch(id = 50, date = "2026-11-22T01:05:00Z", local = "Chivas", visit = "América", teamIdLocal = 2, teamIdVisit = 1)
        val simulator = LeagueSimulator(teams, listOf(match))

        // Without scores: points, then goal difference, goals scored and name (Atlas before Chivas).
        val initial = simulator.table(emptyMap())
        assertEquals(listOf("América", "Atlas", "Chivas"), initial.map { it.team.name })

        // Chivas beats América 2-0 and climbs to first.
        val table = simulator.table(mapOf(50 to SimulatedScore(2, 0)))
        assertEquals(listOf("Chivas", "América", "Atlas"), table.map { it.team.name })
        val chivas = table.first()
        assertEquals(22, chivas.tally.points)
        assertEquals(11, chivas.tally.played)
        assertEquals(1, chivas.movement)
        assertEquals(-1, table[1].movement)
    }

    @Test
    fun randomScoresStayReasonable() {
        repeat(500) {
            val score = LeagueSimulator.randomScore()
            assertTrue(score.local in 0..5 && score.visit in 0..5)
        }
    }
}
