package cohttp.model

import cohttp.CoHttp


/**
 * An API consist of a Request and a ResponseParser.
 * This can be executed and get a response model object.
 */
interface API<ResponseModel> : Request, ResponseParser<ResponseModel> {

    suspend fun execute(context: HTTPContext = HTTPContext.default, configure: ((CoHttp) -> Unit)? = null): ResponseModel {
        val http = CoHttp(this, context)
        configure?.invoke(http)
        return http.toModel(this)
    }
}
