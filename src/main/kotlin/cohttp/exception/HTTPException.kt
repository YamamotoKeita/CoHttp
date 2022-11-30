package cohttp.exception

import cohttp.model.Request
import cohttp.model.Response
import java.io.IOException

class HTTPException(
    val request: Request,
    val type: Type,
    val response: Response? = null,
    cause: Exception? = null,
): IOException(cause) {
    private val responseText: String?

    init {
        responseText = if (response != null && response.isText()) {
            response.text()
        } else {
            null
        }
    }

    override val message: String get() {
        val lines = mutableListOf<String>()
        lines.add("${type.description} [${request.method}] ${request.makeURL()}")

        cause?.let {
            lines.add("Cause $it")
        }

        response?.let {
            lines.add("Status ${it.statusCode}")
            if (responseText != null) {
                lines.add("—–Response—–")
                val omittedText = omit(responseText)
                lines.add(omittedText)
                val endTag = if (responseText == omittedText) "—–End—–" else "—–Omitted—–"
                lines.add(endTag)
            }
        }
        return lines.joinToString(System.lineSeparator())
    }

    /**
     * First 1000 characters and 10 lines
     */
    private fun omit(text: String): String {
        val lineLimit = 10
        val characterLimit = 1000

        val firstText = if (text.length > characterLimit) {
            text.subSequence(0, characterLimit).toString()
        } else {
            text
        }

        val lines = firstText.reader().readLines()
        val firstLines = if (lines.size > lineLimit) {
            lines.subList(0, lineLimit)
        } else {
            lines
        }

        return firstLines.joinToString(System.lineSeparator())
    }

    private fun Response.isText(): Boolean {
        val contentType = header("content-type") ?: return false
        val type = contentType.split(";").firstOrNull()?.trim() ?: return false
        return type.startsWith("text/")
            || type == "application/json"
    }

    enum class Type(val description: String) {
        INVALID_URL         ("The request url is invalid."),

        NETWORK             ("Communication failed. Network may be unstable or the url may be incorrect."),

        INVALID_RESPONSE    ("Response data is not valid."),

        PARSE               ("Failed to parse response."),

        CANCELED            ("Canceled by client.");
    }
}