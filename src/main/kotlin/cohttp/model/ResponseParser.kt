package cohttp.model


/**
 * Convert a raw HTTP response to a model object of application layer.
 */
interface ResponseParser<ResponseModel> {
    /**
     * Validates response before parse.
     * If this method returns "false", an HTTPException is thrown before parse.
     */
    fun validate(response: Response): Boolean = true

    fun parseResponse(response: Response): ResponseModel
}
