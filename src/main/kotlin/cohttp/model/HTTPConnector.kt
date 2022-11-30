package cohttp.model

/**
 * A module to execute HTTP communication.
 * An HTTPConnector is assumed to be created each HTTP communication.
 * You can implement a custom HTTPConnector and apply it via HTTPContext.makeHTTPConnector.
 */
interface HTTPConnector {

    fun execute(request: Request, complete: (Response?, Exception?) -> Unit)

    fun cancel()
}
