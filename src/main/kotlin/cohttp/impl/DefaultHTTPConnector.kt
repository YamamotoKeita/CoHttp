package cohttp.impl

import cohttp.model.HTTPConnector
import cohttp.model.Request
import cohttp.model.Response
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection

class DefaultHTTPConnector : HTTPConnector {

    override fun execute(request: Request): Response {
        var headerFields: Map<String?, List<String>>? = null
        var connection: HttpURLConnection? = null

        return try {
            // Connect
            connection = makeURLConnection(request)
            connection.connect()

            headerFields = connection.headerFields

            makeResponse(connection.responseCode, headerFields, connection.inputStream, connection)
        } catch (e: IOException) {
            if (connection == null || headerFields == null) throw e
            // In some cases such as 404, even if an exception occurs, there is a response data.
            makeResponse(connection.responseCode, headerFields, connection.errorStream, connection)
        }
    }

    private fun makeURLConnection(request: Request): HttpURLConnection {
        val connection = request.makeURL().openConnection() as HttpURLConnection

        connection.requestMethod = request.method.name
        connection.instanceFollowRedirects = request.isRedirectEnabled
        connection.connectTimeout = (request.timeoutSeconds * 1000).toInt()
        connection.readTimeout = (request.timeoutSeconds * 1000).toInt()

        // Add headers
        request.headers.forEach {
            connection.setRequestProperty(it.key, it.value)
        }

        // Set request body
        if (request.body != null) {
            connection.doOutput = true
            connection.outputStream.write(request.body)
        }
        return connection
    }

    private fun makeResponse(statusCode: Int, headerFields: Map<String?, List<String>>, inputStream: InputStream, connection: HttpURLConnection): Response {
        val headers = headerFields.mapNotNull { (key, value) ->
            if (key == null) null else key.lowercase() to value
        }.toMap()

        return HTTPConnectionResponse(
            dataStream = inputStream,
            statusCode = statusCode,
            headers = headers,
            connection = connection
        )
    }

    class HTTPConnectionResponse(
        override val dataStream: InputStream,
        override val statusCode: Int,
        val headers: Map<String, List<String>>,
        val connection: HttpURLConnection,
    ): Response {

        private var dataCache: ByteArray? = null

        override fun body(): ByteArray {
            val cache = dataCache

            return if (cache != null) {
                cache
            } else {
                val data = dataStream.readAllBytes()
                dataCache = data
                data
            }
        }

        override fun headers(name: String): List<String>? {
            return headers[name.lowercase()]
        }

        override fun headerNames(): Set<String> {
            return headers.keys
        }

        override fun close() {
            connection.disconnect()
        }
    }
}