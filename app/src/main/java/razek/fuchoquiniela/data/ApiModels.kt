package razek.fuchoquiniela.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import java.time.Instant

// Types for the fuchoquiniela.com mobile API (/api/v1), shared with the iOS app.
// Keys arrive in snake_case and are mapped with JsonNamingStrategy.SnakeCase (see FuchoApi.json),
// so property names are the camelCase form. Dates stay strings and are read with parseDate().

@Serializable
data class User(
    val id: Int,
    val name: String,
    val nickname: String? = null,
    val email: String,
) {
    val displayName: String get() = nickname?.takeIf { it.isNotEmpty() } ?: name
}

@Serializable
data class LoginResponse(val token: String, val user: User)

@Serializable
data class UserResponse(val user: User)

@Serializable
data class Pool(
    val uuid: String,
    val name: String,
    val description: String? = null,
    /** Laravel serializes decimal columns as strings ("100.00"); see [priceValue]. */
    val price: JsonElement? = null,
    val league: String? = null,
    val season: String? = null,
    val seasonId: Int = 0,
    val isAdmin: Boolean = false,
    val createdAt: String? = null,
    val participantsCount: Int? = null,
) {
    val subtitle: String get() = listOfNotNull(league, season).filter { it.isNotEmpty() }.joinToString(" · ")

    val priceValue: Double
        get() = (price as? JsonPrimitive)?.let { it.doubleOrNull ?: it.content.toDoubleOrNull() } ?: 0.0

    val created: Instant? get() = createdAt?.let(::parseDate)

    /** The website's invite link. Whoever opens it joins after signing in, on the web or in the app. */
    val inviteUrl: String get() = "$SITE_URL/pool/$uuid/join"
}

@Serializable
data class PoolsResponse(val pools: List<Pool>)

@Serializable
data class PoolResponse(val pool: Pool)

@Serializable
data class PickScore(val scoreLocal: Int, val scoreVisit: Int)

@Serializable
data class Match(
    val id: Int,
    val roundId: Int,
    val round: String,
    val date: String,
    val local: String,
    val visit: String,
    val localShortname: String? = null,
    val visitShortname: String? = null,
    val teamIdLocal: Int = 0,
    val teamIdVisit: Int = 0,
    val imageLocalUrl: String? = null,
    val imageVisitUrl: String? = null,
    val scoreLocal: Int? = null,
    val scoreVisit: Int? = null,
    val isLive: Boolean = false,
    val liveStatus: String? = null,
    val locked: Boolean = false,
    val pick: PickScore? = null,
) {
    val kickoff: Instant get() = parseDate(date) ?: Instant.EPOCH
    val hasResult: Boolean get() = scoreLocal != null && scoreVisit != null
}

@Serializable
data class MatchesResponse(val serverTime: String? = null, val matches: List<Match>)

@Serializable
data class PickInput(val matchId: Int, val scoreLocal: Int, val scoreVisit: Int)

@Serializable
data class SavePicksRequest(val picks: List<PickInput>)

@Serializable
data class SavePicksResponse(
    val saved: List<Int> = emptyList(),
    val rejected: List<Rejection> = emptyList(),
) {
    @Serializable
    data class Rejection(
        /** Null when the server couldn't read the item's match_id (reason "invalid"). */
        val matchId: Int? = null,
        val reason: String,
    )
}

@Serializable
data class Standing(
    val place: Int,
    val poolUserId: Int,
    val name: String? = null,
    val nickname: String? = null,
    val points: Int,
    val exactHits: Int,
    val resultHits: Int,
    val evaluatedPicks: Int = 0,
    val isMe: Boolean = false,
) {
    val displayName: String get() = displayName(name, nickname)
}

@Serializable
data class PositionsResponse(val completedMatchesCount: Int, val positions: List<Standing>)

// Pool information and round detail

@Serializable
data class Participant(
    val poolUserId: Int,
    val name: String? = null,
    val nickname: String? = null,
    val isOwner: Boolean = false,
    val isMe: Boolean = false,
    val joinedAt: String? = null,
) {
    val displayName: String get() = displayName(name, nickname)
}

@Serializable
data class ParticipantsResponse(val participants: List<Participant>)

@Serializable
data class RoundRef(val id: Int, val name: String)

@Serializable
data class RoundsResponse(val currentRoundId: Int? = null, val rounds: List<RoundRef>)

@Serializable
data class RoundDetailResponse(
    val round: RoundRef,
    val participants: List<Member>,
    val matches: List<RoundMatch>,
) {
    @Serializable
    data class Member(
        val poolUserId: Int,
        val name: String? = null,
        val nickname: String? = null,
        val isMe: Boolean = false,
        /** Points earned in this round, computed by the server. */
        val points: Int = 0,
    ) {
        val displayName: String get() = displayName(name, nickname)
    }

    @Serializable
    data class MemberPick(
        val poolUserId: Int,
        val scoreLocal: Int,
        val scoreVisit: Int,
        /** Null until the match has a score. */
        val points: Int? = null,
    )

    @Serializable
    data class RoundMatch(
        val id: Int,
        val date: String,
        val local: String,
        val visit: String,
        val localShortname: String? = null,
        val visitShortname: String? = null,
        val imageLocalUrl: String? = null,
        val imageVisitUrl: String? = null,
        val scoreLocal: Int? = null,
        val scoreVisit: Int? = null,
        val isLive: Boolean = false,
        val liveStatus: String? = null,
        val locked: Boolean = false,
        /** Only the user's own pick until the match locks. */
        val picks: List<MemberPick> = emptyList(),
    ) {
        val kickoff: Instant get() = parseDate(date) ?: Instant.EPOCH
        val hasResult: Boolean get() = scoreLocal != null && scoreVisit != null
    }
}

// Public sections

@Serializable
data class Season(val id: Int, val name: String, val league: String) {
    val title: String get() = "$league $name"
}

@Serializable
data class TeamRef(val id: Int, val name: String, val shortname: String? = null, val imageUrl: String? = null)

/** Wins, draws, losses and goals, overall or at home/away. */
@Serializable
data class Tally(
    val played: Int = 0,
    val won: Int = 0,
    val drawn: Int = 0,
    val lost: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    val points: Int = 0,
) {
    fun adding(goalsFor: Int, goalsAgainst: Int): Tally = copy(
        played = played + 1,
        goalsFor = this.goalsFor + goalsFor,
        goalsAgainst = this.goalsAgainst + goalsAgainst,
        won = won + if (goalsFor > goalsAgainst) 1 else 0,
        drawn = drawn + if (goalsFor == goalsAgainst) 1 else 0,
        lost = lost + if (goalsFor < goalsAgainst) 1 else 0,
        points = points + when {
            goalsFor > goalsAgainst -> 3
            goalsFor == goalsAgainst -> 1
            else -> 0
        },
    )
}

@Serializable
data class FormGame(
    /** W, D or L. */
    val result: String,
    val score: String,
    val rival: String,
)

/** One row of a league table. `form`, `home` and `away` come only with /standings. */
@Serializable
data class TableRow(
    val position: Int,
    val team: TeamRef,
    val goalDiff: Int,
    val played: Int,
    val won: Int,
    val drawn: Int,
    val lost: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val points: Int,
    val form: List<FormGame>? = null,
    val home: Tally? = null,
    val away: Tally? = null,
) {
    val tally: Tally get() = Tally(played, won, drawn, lost, goalsFor, goalsAgainst, points)
}

@Serializable
data class StandingsResponse(
    val season: Season,
    val qualified: Int,
    val finishedMatches: Int,
    val totalMatches: Int,
    val rows: List<TableRow>,
)

/** A match outside any pool (match list and simulator). */
@Serializable
data class LeagueMatch(
    val id: Int,
    val roundId: Int = 0,
    val round: String = "",
    val date: String,
    val local: String,
    val visit: String,
    val teamIdLocal: Int? = null,
    val teamIdVisit: Int? = null,
    val imageLocalUrl: String? = null,
    val imageVisitUrl: String? = null,
    val scoreLocal: Int? = null,
    val scoreVisit: Int? = null,
    val isLive: Boolean = false,
    val liveStatus: String? = null,
    val season: String? = null,
) {
    val kickoff: Instant get() = parseDate(date) ?: Instant.EPOCH
    val hasResult: Boolean get() = scoreLocal != null && scoreVisit != null
}

@Serializable
data class LeagueMatchesResponse(val matches: List<LeagueMatch>, val page: Int, val hasMore: Boolean)

@Serializable
data class SimulatorResponse(
    val season: Season,
    val qualified: Int,
    val teams: List<TableRow>,
    val rounds: List<Round>,
) {
    @Serializable
    data class Round(val id: Int, val name: String, val matches: List<LeagueMatch>)
}

@Serializable
data class Post(
    val id: Int,
    val title: String,
    val excerpt: String = "",
    val imageUrl: String? = null,
    val thumbnailUrl: String? = null,
    val author: String? = null,
    val publishedAt: String? = null,
    val url: String = "",
    /** Only in the detail response. */
    val bodyHtml: String? = null,
    val link: String? = null,
) {
    val published: Instant? get() = publishedAt?.let(::parseDate)
}

@Serializable
data class PostsResponse(val posts: List<Post>, val page: Int, val hasMore: Boolean)

@Serializable
data class PostResponse(val post: Post)

@Serializable
data class ApiErrorBody(val message: String? = null)

private fun displayName(name: String?, nickname: String?): String =
    nickname?.takeIf { it.isNotEmpty() } ?: name ?: "—"
