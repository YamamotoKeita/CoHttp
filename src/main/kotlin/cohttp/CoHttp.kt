package cohttp


import cohttp.enumtype.Method
import cohttp.exception.HTTPException
import cohttp.model.*
import kotlinx.coroutines.*
import java.net.URL
import java.nio.charset.Charset
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class CoHttp(
    private val mutableRequest: MutableRequest,
    val context: HTTPContext = HTTPContext.default
): CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.IO

    private var job: Job? = null

    private val httpConnector = context.makeHTTPConnector()

    private var validate: ((Response) -> Boolean)? = null

    private var onReceived: ((Response) -> Unit)? = null

    private var continuation: CancellableContinuation<*>? = null

    constructor(request: Request, context: HTTPContext = HTTPContext.default)
        : this(mutableRequest = MutableRequest(request), context = context)

    constructor(url: String, context: HTTPContext = HTTPContext.default)
        : this(mutableRequest = MutableRequest(url), context = context)

    fun method(method: Method): CoHttp {
        mutableRequest.method = method
        return this
    }

    fun headers(headers: Map<String, String>): CoHttp {
        mutableRequest.mutableHeaders = headers.toMutableMap()
        return this
    }

    fun header(key: String, value: String): CoHttp {
        mutableRequest.mutableHeaders[key] = value
        return this
    }

    fun queries(queries: Map<String, String>) : CoHttp {
        mutableRequest.urlQuery = URLQuery(queries)
        return this
    }

    fun queries(urlQuery: URLQuery) : CoHttp {
        mutableRequest.urlQuery = urlQuery
        return this
    }

    fun query(name: String, value: String?) : CoHttp {
        mutableRequest.urlQuery[name] = value
        return this
    }

    fun body(body: ByteArray): CoHttp {
        mutableRequest.body = body
        return this
    }

    fun timeoutSeconds(timeoutSeconds: Double): CoHttp {
        mutableRequest.timeoutSeconds = timeoutSeconds
        return this
    }

    fun redirectEnabled(isRedirectEnabled: Boolean): CoHttp {
        mutableRequest.isRedirectEnabled = isRedirectEnabled
        return this
    }

    fun validate(validate: (Response) -> Boolean): CoHttp {
        this.validate = validate
        return this
    }

    fun onReceived(onReceived: (Response) -> Unit): CoHttp {
        this.onReceived = onReceived
        return this
    }

    /**
     * Use the response, then close the response
     */
    suspend fun <T> useResponse(use: (Response) -> T): T = suspendCancellableCoroutine { continuation ->
        this.continuation = continuation

        val url: URL
        try {
            url = mutableRequest.makeURL()
        } catch (e: Exception) {
            val httpException = HTTPException(mutableRequest, HTTPException.Type.INVALID_URL, null, e)
            continuation.resumeWithException(httpException)
            return@suspendCancellableCoroutine
        }

        // Add cookies
        if (context.isCookieEnabled) {
            val cookies = context.cookieManager.cookieStore.get(url.toURI())
            if (cookies.isNotEmpty()) {
                // HTTP/2 allows multiple values on the same header name, but HTTP/1.1 doesn't allow that.
                // So cookie values are joined to one string by semicolon.
                // see https://www.rfc-editor.org/rfc/rfc7540#section-8.1.2.5
                val joinedCookie = cookies.joinToString("; ") { it.toString() }
                mutableRequest.mutableHeaders["Cookie"] = joinedCookie
            }
        }

        context.logInfo("[${mutableRequest.method}] $url")

        this.job = launch {
            try {
                val response = httpConnector.execute(mutableRequest)
                response.use {
                    if (context.isCookieEnabled) {
                        val headers = response.headerNames().mapNotNull { key ->
                            val values = response.headers(key) ?: return@mapNotNull null
                            key to values
                        }.toMap()

                        context.cookieManager.put(url.toURI(), headers)
                    }

                    onReceived?.invoke(response)

                    val validate = this@CoHttp.validate
                    if (validate != null && !validate(response)) {
                        continuation.resumeWithException(HTTPException(mutableRequest, HTTPException.Type.INVALID_RESPONSE, response))
                        return@use
                    }

                    if (job?.isCancelled == true) {
                        return@use
                    }
                    try {
                        val result = use(response)
                        continuation.resume(result)
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }
            } catch (e: Exception) {
                val httpException = HTTPException(mutableRequest, HTTPException.Type.NETWORK, null, e)
                continuation.resumeWithException(httpException)
            }
        }
    }

    suspend fun toText(charset: Charset? = null): String {
        return useResponse { it.text(charset) }
    }

    suspend fun toByteArray(): ByteArray = useResponse{ it.body() }

    suspend fun <T> toModel(parse: (Response) -> T): T {
        return useResponse {
            try {
                parse(it)
            } catch (e: Exception) {
                throw HTTPException(mutableRequest, HTTPException.Type.PARSE, it, e)
            }
        }
    }

    suspend fun <T> toModel(responseParser: ResponseParser<T>): T {
        return useResponse {
            if (!responseParser.validate(it)) {
                throw HTTPException(mutableRequest, HTTPException.Type.INVALID_RESPONSE, it)
            }

            try {
                responseParser.parseResponse(it)
            } catch (e: Exception) {
                throw HTTPException(mutableRequest, HTTPException.Type.PARSE, it, e)
            }
        }
    }

    fun cancel() {
        try {
            job?.cancel()
        } catch (t: Throwable) {
            context.logger.warn(t)
        }
        continuation?.resumeWithException(HTTPException(mutableRequest, HTTPException.Type.CANCELED))
    }
}
