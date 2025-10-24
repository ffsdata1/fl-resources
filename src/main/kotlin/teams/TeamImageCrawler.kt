package teams

import Constant
import extension.getEnv
import kotlinx.serialization.json.Json
import utils.ImageCrawlerUtil
import java.io.File

object TeamImageCrawler {
    private const val teamImagePath = "assets/image/teams/"
    private const val teamStaticImagePath = "assets/static/teams/"
    private const val stageBadgeImagePath = "assets/image/competitions/"

    private val json = Json { ignoreUnknownKeys = true }

    val staticImageTeams = mutableMapOf<String, String>()
    val teamSquad = mutableMapOf<String, Long>()

    fun fetchTeamImages() {
        initData()
        val teams = mutableListOf<Team>()
        val stageBadges = mutableListOf<StageBadge>()
        (0 until (getEnv(Constant.ENV_DATE_COUNT_TO_FETCH_TEAMS)?.toInt() ?: 1)).forEach {
            val crawData = TeamExtractor.fetchCrawData(it)
            teams.addAll(crawData.teams)
            stageBadges.addAll(crawData.stageBadges)
        }
        val distinctTeams = teams
            .filter { !it.Img.isNullOrBlank() }
            .distinctBy { it.ID }
        distinctTeams.forEachIndexed { index, team ->
            if (!teamSquad.contains(team.ID)) {
                teamSquad[team.ID] = 0
            }
            crawTeamImage(
                team.ID,
                team.Nm,
                staticImageTeams[team.ID],
                team.Img.orEmpty(),
                String.format("%04d/%04d", index + 1, distinctTeams.size)
            )
        }

        val distinctStages = stageBadges
            .filter { !it.badge.isNullOrBlank() }
            .distinctBy { it.Sid }
        distinctStages.forEachIndexed { index, stageBadge ->
            crawStageBadge(
                stageBadge.Sid,
                stageBadge.badge.orEmpty(),
                String.format("%04d/%04d", index + 1, distinctStages.size)
            )
        }
    }

    fun initData() {
        val staticImgJson = File("assets/config/static-map.json").readText()
        val teamSquadJson = File("assets/config/team-squad.json").readText()
        staticImageTeams.clear()
        staticImageTeams.putAll(json.decodeFromString<Map<String, String>>(staticImgJson).filter {
            it.value.isNotBlank()
        })
        teamSquad.clear()
        teamSquad.putAll(
            json.decodeFromString<Map<String, Long>>(teamSquadJson)
        )
    }

    private fun crawTeamImage(ID: String, Nm: String, StaticImg: String?, Img: String, index: String) {
        val destinationPath = "$teamImagePath$Img"
        val destinationFile = File(destinationPath)

        // Copy from StaticImg if it's not null or empty
        if (!StaticImg.isNullOrBlank()) {
            val sourceFile = File("$teamStaticImagePath$StaticImg")
            if (sourceFile.exists()) {
                sourceFile.copyTo(destinationFile, overwrite = true)
                println("$index CRAW SUCCESS - STATIC :$ID $Nm $StaticImg to $Img")
                return
            } else {
                println("$index CRAW STATIC FAIL WITH NO STATIC FILE: $ID $Nm $StaticImg to $Img")
            }
        }

        val imagesBaseUrl = "${getEnv(Constant.ENV_IMAGES_BASE_URL)}"
        val qualityPaths = listOf(
            "3xl",
            "high",
            "medium"
        )
        var crawResult = false
        for (quality in qualityPaths) {
            val imageUrl = "$imagesBaseUrl/team/$quality/$Img"
            crawResult = ImageCrawlerUtil.crawlImage(imageUrl, destinationPath)
            if (crawResult) {
                println("$index TEAM IMAGE CRAW SUCCESS - ${quality.uppercase()} QUALITY: $ID $Nm $Img")
                break
            }
        }
        if (!crawResult) {
            println("$index \uD83D\uDD25 \uD83D\uDD25 \uD83D\uDD25 TEAM IMAGE CRAW FAIL: $ID $Nm $Img")
        }

        // Try to crawl high-quality image
//        val highQualityUrl = "${getEnv(Constant.ENV_HIGH_QUALITY_URL)}$Img"
//        val highQualitySuccess = ImageCrawlerUtil.crawlImage(highQualityUrl, destinationPath)
//        if (highQualitySuccess) {
//            println("$index CRAW SUCCESS - HIGH QUALITY: $ID $Nm $Img")
//        } else {
//            // If high-quality fails, try medium-quality
//            val mediumQualityUrl = "${getEnv(Constant.ENV_MEDIUM_QUALITY_URL)}$Img"
//            val mediumQualitySuccess = ImageCrawlerUtil.crawlImage(mediumQualityUrl, destinationPath)
//            if (mediumQualitySuccess) {
//                println("$index CRAW SUCCESS - MEDIUM QUALITY: $ID $Nm $Img")
//            } else {
//                println("$index \uD83D\uDD25 \uD83D\uDD25 \uD83D\uDD25CRAW FAIL: $ID $Nm $Img")
//            }
//        }
    }

    private fun crawStageBadge(id: String, badge: String, index: String) {
        val destinationPath = "$stageBadgeImagePath$badge"

        val imagesBaseUrl = "${getEnv(Constant.ENV_IMAGES_BASE_URL)}"
        val qualityPaths = listOf(
            "3xl",
            "high",
            "medium"
        )

        var crawResult = false
        for (quality in qualityPaths) {
            val imageUrl = "$imagesBaseUrl/competition/$quality/$badge"
            crawResult = ImageCrawlerUtil.crawlImage(imageUrl, destinationPath)
            if (crawResult) {
                println("$index COMPETITION IMAGE CRAW SUCCESS - ${quality.uppercase()} QUALITY: $id $badge")
                break
            }
        }
        if (!crawResult) {
            println("$index \uD83D\uDD25 \uD83D\uDD25 \uD83D\uDD25 COMPETITION IMAGE CRAW FAIL: $id $badge")
        }

//        val highQualityUrl = "${getEnv(Constant.ENV_STAGE_HIGH_QUALITY_URL)}$badge"
//        val success = ImageCrawlerUtil.crawlImage(highQualityUrl, destinationPath)
//        if (success) {
//            println("$index CRAW SUCCESS - STAGE BADGE - HIGH QUALITY: $id $badge $index")
//        } else {
//            val mediumUrl = "${getEnv(Constant.ENV_STAGE_MEDIUM_QUALITY_URL)}$badge"
//            val mediumSuccess = ImageCrawlerUtil.crawlImage(mediumUrl, destinationPath)
//            if (mediumSuccess) {
//                println("$index CRAW SUCCESS - STAGE BADGE - MEDIUM QUALITY: $id $badge $index")
//            } else {
//                println("$index \uD83D\uDD25 \uD83D\uDD25 \uD83D\uDD25CRAW FAIL: $id $badge $index")
//            }
//        }
    }
}
