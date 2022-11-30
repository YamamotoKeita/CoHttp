package cohttp.model

import cohttp.impl.DefaultHTTPConnector
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import java.net.CookieManager

/**
 * A configuration assumed to be shared by HTTP communications.
 */
data class HTTPContext(
    /** An HTTPConnector is created by this method for each HTTP communication. You can implement a custom HTTPConnector and apply it by this method. */
    var makeHTTPConnector: () -> HTTPConnector = { DefaultHTTPConnector() },

    /** If this is true, logs are output when an HTTP communication starts or an error occurs. */
    var isLogEnabled: Boolean = true,

    /** A logger for output logs. You can change it to any other logger. */
    var logger: Log = LogFactory.getLog(javaClass),

    /** If this is true, cookies are saved and send. */
    var isCookieEnabled: Boolean = true,

    /** Store of cookies */
    var cookieManager: CookieManager = CookieManager(),

) {
    fun logInfo(message: Any) {
        if (isLogEnabled) {
            logger.info(message)
        }
    }

    companion object {
        var default = HTTPContext()
    }
}
