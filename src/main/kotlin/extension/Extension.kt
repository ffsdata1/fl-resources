package extension

import io.github.cdimascio.dotenv.dotenv

fun getEnv(key: String): String? {
    val isGithubCI = System.getenv("GITHUB_ACTIONS") == "true"

    return if (isGithubCI) {
        System.getenv(key) // dùng GitHub Secrets
    } else {
        dotenv()[key]      // dùng .env local
    }
}
