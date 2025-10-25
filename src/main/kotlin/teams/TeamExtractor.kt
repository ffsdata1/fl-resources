package teams

import Constant
import extension.getEnv
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
data class Team(val ID: String, val Nm: String, val Img: String? = null)

@Serializable
data class Event(val T1: List<Team> = emptyList(), val T2: List<Team> = emptyList())

@Serializable
data class Stage(val Sid: String, val badgeUrl: String? = null, val Events: List<Event> = emptyList())

data class StageBadge(val Sid: String, val badge: String?)

data class CrawData(
    val teams: List<Team>,
    val stageBadges: List<StageBadge>
)

@Serializable
data class Root(val Stages: List<Stage> = emptyList())

@Serializable
data class Player(
    val Pid: String,
    val Pnm: String? = null,
    val ImageUrl: String? = null
)

@Serializable
data class TeamSquad(
    val ID: String,
    val Ps: List<Player> = emptyList()
)

object TeamExtractor {

    private val client = OkHttpClient.Builder().build()
    private val json = Json { ignoreUnknownKeys = true }

    fun fetchCrawData(dayOffset: Int): CrawData {
        val url = generateUrl(dayOffset)
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Unexpected code $response")
            val body = response.body?.string() ?: return CrawData(emptyList(), emptyList())
            val data = json.decodeFromString<Root>(body)
            return CrawData(
                teams = data.Stages.flatMap { stage ->
                    stage.Events.flatMap { event ->
                        event.T1 + event.T2
                    }
                },
                stageBadges = data.Stages.map { stage ->
                    StageBadge(stage.Sid, stage.badgeUrl)
                }
            )
        }
    }

    private fun generateUrl(dayOffset: Int): String {
        val date = LocalDate.now().plusDays(dayOffset.toLong())
        val formatted = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        return "${getEnv(Constant.ENV_BASE_API_URL)}/$formatted/0?MD=0"
    }

    fun fetchTeamSquad(teamId: String): TeamSquad {
        try {
            val url = "${getEnv(Constant.ENV_TEAM_DETAIL_API)}/$teamId/squad?locale=en"

            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Unexpected code $response")
                val body = response.body?.string() ?: return TeamSquad(teamId, emptyList())
                val data = json.decodeFromString<TeamSquad>(body)
                return data
            }
        } catch (e: Throwable) {
            return TeamSquad(teamId, emptyList())
        }
    }

    private fun fetchJsonFromUrl(url: String): String {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().let { response ->
            if (!response.isSuccessful) {
                throw Exception("Failed to fetch data from $url. HTTP ${response.code}")
            }
            return response.body?.string() ?: throw Exception("Empty response body")
        }
        throw Exception("Failed to fetch data from $url")
    }
}