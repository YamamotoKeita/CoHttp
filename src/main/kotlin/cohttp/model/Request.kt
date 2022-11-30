package cohttp.model

import cohttp.enumtype.Method
import java.net.URL


/**
 * Contents of an HTTP request including URL, HTTP method, headers, body and others.
 */
interface Request {
    /** A URL to connect */
    val url: String

    /** An HTTP request method such as GET, POST */
    val method: Method get() = default.method

    /** HTTP request headers */
    val headers: Map<String, String> get() = default.headers

    /** Query parameters that is added to the request URL */
    val urlQuery: URLQuery get() = default.urlQuery

    /** The body data to post */
    val body: ByteArray? get() = default.body

    /** Timeout interval of data transfer (seconds) */
    val timeoutSeconds: Double get() = default.timeoutSeconds

    /** If this is true, HTTP communication is automatically redirected according to redirection information in the HTTP header. */
    val isRedirectEnabled: Boolean  get() =  default.isRedirectEnabled

    fun makeURL(): URL {
        if (urlQuery.isEmpty()) {
            return URL(url)
        }

        val separator = if (url.contains("?")) "&" else "?"
        val urlStr = url + separator + urlQuery.queryString()
        return URL(urlStr)
    }

    companion object {
        val default: Request = object : Request {
            override val url: String get() = throw IllegalAccessException("url doesn't have a default value.")
            override val method: Method get() = Method.GET
            override val headers: Map<String, String> get() = mapOf()
            override val urlQuery: URLQuery get() = URLQuery()
            override val body: ByteArray? get() = null
            override val timeoutSeconds: Double get() = 15.0
            override val isRedirectEnabled: Boolean  get() =  true
        }
    }
}
