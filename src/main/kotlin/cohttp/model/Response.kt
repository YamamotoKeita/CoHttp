package cohttp.model

import java.io.Closeable
import java.io.InputStream
import java.nio.charset.Charset

/**
 * Representation of an HTTP response
 */
interface Response: Closeable {
    val statusCode: Int
    val dataStream: InputStream

    /**
     * Get response body data.
     * An implementation of this method can cache the data for better performance.
     */
    fun body(): ByteArray

    /**
     * Get all header values for [name].
     */
    fun headers(name: String): List<String>?

    /**
     * Get all header names.
     */
    fun headerNames(): Set<String>

    /**
     * Get a header value for [name].
     * If there are multiple values with the same name, this returns the first one of them.
     * Generally, only the "Set-Cookie" header has multiple values, so it is almost safe to assume that the other headers have only one value.
     */
    fun header(name: String): String? {
        val headers = headers(name) ?: return null
        return headers.first()
    }

    /**
     * Get a response text.
     * If [charset] is null, this use a charset in the "Content-Type" header.
     */
    fun text(charset: Charset? = null): String {
        val encoding = charset ?: charset() ?: Charsets.UTF_8
        return body().toString(encoding)
    }

    /**
     * Get all header lines.
     */
    fun allHeaders(): List<Pair<String, String>> {
        return headerNames().flatMap { name ->
            headers(name)?.map { value ->
                name to value
            } ?: listOf(name to "")
        }
    }

    /**
     * Get the charset in the "Content-Type" header.
     */
    fun charset(): Charset? {
        val contentType = header("content-type") ?: return null

        val charsetText = contentType
            .split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("charset=", true) }
            ?: return null

        val charset = charsetText.substring(charsetText.indexOf("=") + 1)
        return Charset.forName(charset)
    }

}
