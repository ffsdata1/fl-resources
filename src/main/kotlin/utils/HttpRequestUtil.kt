package utils

import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object HttpRequestUtil {

    /**
     * Sends a POST request with a JSON body built from [bodyMap] and optional [headers].
     * Returns a Pair(success, errorMessage):
     *   - success = true and errorMessage = null when HTTP 2xx
     *   - success = false and errorMessage contains the status code + response body on failure
     */
    fun doPost(
        endpoint: String,
        bodyMap: Map<String, Any>,
        headers: Map<String, String> = emptyMap()
    ): Pair<Boolean, String?> {
        return try {
            val url = URL(endpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }

            val jsonBody = buildJsonBody(bodyMap)
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                connection.disconnect()
                Pair(true, null)
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.readText()
                    ?: connection.inputStream?.bufferedReader()?.readText()
                    ?: "(no body)"
                connection.disconnect()
                Pair(false, "HTTP $responseCode: $errorBody")
            }
        } catch (e: Exception) {
            Pair(false, "Exception: ${e.message}")
        }
    }

    /** Minimal JSON serialiser for Map<String, Any> (supports String, List<String>, and nested maps). */
    private fun buildJsonBody(map: Map<String, Any>): String {
        val sb = StringBuilder("{")
        map.entries.forEachIndexed { index, (key, value) ->
            if (index > 0) sb.append(",")
            sb.append("\"$key\":")
            sb.append(toJsonValue(value))
        }
        sb.append("}")
        return sb.toString()
    }

    private fun toJsonValue(value: Any?): String = when (value) {
        null -> "null"
        is String -> "\"${value.replace("\"", "\\\"")}\""
        is Number, is Boolean -> value.toString()
        is List<*> -> "[${value.joinToString(",") { toJsonValue(it) }}]"
        is Map<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            buildJsonBody(value as Map<String, Any>)
        }
        else -> "\"$value\""
    }
}
