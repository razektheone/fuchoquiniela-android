package razek.fuchoquiniela.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.math.exp
import kotlin.random.Random

// The Liguilla simulator's table: the current standings plus the scores the user typed for the
// matches still to play. Mirrors the web simulator (fuchito resources/views/simulator.blade.php)
// and the iOS app: 3 points per win, 1 per draw, ordered by points, goal difference, goals scored, name.

@Serializable
data class SimulatedScore(val local: Int, val visit: Int)

data class SimulatedRow(
    val team: TeamRef,
    /** Position in the real table, to show how many places the team moved. */
    val currentPosition: Int,
    val position: Int,
    val tally: Tally,
) {
    val goalDiff: Int get() = tally.goalsFor - tally.goalsAgainst

    /** Positive when the team climbed. */
    val movement: Int get() = currentPosition - position
}

class LeagueSimulator(private val teams: List<TableRow>, private val matches: List<LeagueMatch>) {

    fun table(scores: Map<Int, SimulatedScore>): List<SimulatedRow> {
        val rows = teams.associate { it.team.id to SimulatedRow(it.team, it.position, it.position, it.tally) }.toMutableMap()

        for (match in matches) {
            val score = scores[match.id] ?: continue
            val local = match.teamIdLocal ?: continue
            val visit = match.teamIdVisit ?: continue
            rows[local]?.let { rows[local] = it.copy(tally = it.tally.adding(score.local, score.visit)) }
            rows[visit]?.let { rows[visit] = it.copy(tally = it.tally.adding(score.visit, score.local)) }
        }

        return rows.values
            .sortedWith(
                compareByDescending<SimulatedRow> { it.tally.points }
                    .thenByDescending { it.goalDiff }
                    .thenByDescending { it.tally.goalsFor }
                    .thenBy { it.team.name },
            )
            .mapIndexed { index, row -> row.copy(position = index + 1) }
    }

    companion object {
        const val MAX_GOALS = 20

        /** Goals from a Poisson distribution capped at 5, like the web's "Aleatorio". The home side scores a little more. */
        fun randomScore(): SimulatedScore = SimulatedScore(poisson(1.45), poisson(1.1))

        private fun poisson(mean: Double): Int {
            val limit = exp(-mean)
            var goals = 0
            var product = Random.nextDouble()
            while (product > limit && goals < 5) {
                goals += 1
                product *= Random.nextDouble()
            }
            return goals
        }
    }
}

/** Simulated scores survive relaunches, per season, like the web's localStorage. Never sent to the server. */
object SimulatorStore {
    private fun prefs(context: Context) = context.getSharedPreferences("simulator", Context.MODE_PRIVATE)
    private fun key(season: Int) = "simulator.scores.$season"

    fun load(context: Context, season: Int): Map<Int, SimulatedScore> {
        val text = prefs(context).getString(key(season), null) ?: return emptyMap()
        return runCatching {
            FuchoApi.json.decodeFromString<Map<String, SimulatedScore>>(text)
                .mapNotNull { (key, value) -> key.toIntOrNull()?.let { it to value } }
                .toMap()
        }.getOrDefault(emptyMap())
    }

    fun save(context: Context, season: Int, scores: Map<Int, SimulatedScore>) {
        val text = FuchoApi.json.encodeToString(scores.mapKeys { it.key.toString() })
        prefs(context).edit().putString(key(season), text).apply()
    }
}
