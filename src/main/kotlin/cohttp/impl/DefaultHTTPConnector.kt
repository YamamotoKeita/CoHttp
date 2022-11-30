package cohttp.impl

import cohttp.model.HTTPConnector
import cohttp.model.Request
import cohttp.model.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection

class DefaultHTTPConnector : HTTPConnector {

    val coroutineScope = CoroutineScope(Dispatchers.IO)

    private var httpTask: HTTPTask? = null

    override fun execute(request: Request, complete: (Response?, Exception?) -> Unit) {
        httpTask = HTTPTask(complete)
        httpTask?.execute(request)
    }

    override fun cancel() {
        httpTask?.cancel()
    }

    private inner class HTTPTask(private val complete: (Response?, Exception?) -> Unit) {

        private var connection: HttpURLConnection? = null
        private var job: Job? = null

        fun execute(request: Request) {
            job = coroutineScope.launch {
                val response: Response?
                try {
                    response = connect(request = request)
                } catch (e: Exception) {
                    complete(null, e)
                    return@launch
                }

                if (job?.isCancelled == false) {
                    complete(response, null)
                }
            }
        }

        fun cancel() {
            connection?.disconnect()
            job?.cancel()
        }

        private fun connect(request: Request): Response? {
            var headerFields: Map<String?, List<String>>? = null
            var connection: HttpURLConnection? = null

            try {
                // Connect
                connection = makeURLConnection(request)
                this.connection = connection
                connection.connect()

                headerFields = connection.headerFields

                val response = makeResponse(connection.responseCode, headerFields, connection.inputStream, connection)

                return response
            } catch (e: IOException) {
                if (connection == null) throw e
                // In some cases such as 404, even if an exception occurs, there is a response data.
                return makeResponse(connection.responseCode, headerFields, connection.errorStream, connection) ?: throw e
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

        private fun makeResponse(statusCode: Int, headerFields: Map<String?, List<String>>?, inputStream: InputStream?, connection: HttpURLConnection): Response? {
            val headerFields = headerFields ?: return null
            val inputStream = inputStream ?: return null

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