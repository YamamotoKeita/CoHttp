package cohttp.model

import cohttp.enumtype.Method

/**
 * A mutable representation of an HTTP request.
 */
class MutableRequest(
    override var url: String,
    override var method: Method,
    override var urlQuery: URLQuery,
    override var body: ByteArray?,
    override var timeoutSeconds: Double,
    override var isRedirectEnabled: Boolean,
    var mutableHeaders: MutableMap<String, String>,
): Request {

    override val headers: Map<String, String> get() = mutableHeaders

    constructor(spec: Request): this(
        url = spec.url,
        method = spec.method,
        urlQuery = spec.urlQuery,
        body = spec.body,
        timeoutSeconds = spec.timeoutSeconds,
        isRedirectEnabled = spec.isRedirectEnabled,
        mutableHeaders = spec.headers.toMutableMap(),
    )

    constructor(url: String): this(
        url = url,
        method = Request.default.method,
        urlQuery = Request.default.urlQuery,
        body = Request.default.body,
        timeoutSeconds = Request.default.timeoutSeconds,
        isRedirectEnabled = Request.default.isRedirectEnabled,
        mutableHeaders = Request.default.headers.toMutableMap(),
    )
}
