package cohttp.model

/**
 * TODO Now HTTPConnector launch and cancel the coroutine, but instead, CoHttp should do it. Then this interface can be more simple.
 *
 * A module to execute HTTP communication.
 * An HTTPConnector is assumed to be created each HTTP communication.
 * You can implement a custom HTTPConnector and apply it via HTTPContext.makeHTTPConnector.
 */
interface HTTPConnector {

    fun execute(request: Request, complete: (Response?, Exception?) -> Unit)

    fun cancel()
}
