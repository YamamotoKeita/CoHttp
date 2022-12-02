package cohttp.model

/**
 * A module to execute HTTP communication.
 * An HTTPConnector is assumed to be created each HTTP communication.
 * You can implement a custom HTTPConnector and apply it via HTTPContext.makeHTTPConnector.
 */
interface HTTPConnector {
    /**
     * Executes an HTTP communication synchronously and returns a response object.
     * Otherwise, throws an exception.
     */
    fun execute(request: Request): Response
}
