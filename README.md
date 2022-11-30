
## Features

This is an HTTP library using coroutine. The main objectives of this library are the followings.

- Executes HTTP communication with a `suspend function` of coroutine
- Turns an HTTP API into a class
- Offers easy interface to call HTTP request

This library can be used on its own or in combination with another HTTP library such as OkHttp.
When you use this library with another library, another library executes HTTP communications and this library works as a wrapper for it.

## Installation

TODO

## Requirements
Kotlin Coroutines and Commons Logging libraries are necessary.

```kotlin
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
implementation("commons-logging:commons-logging:1.2")
```

## Simplest examples

### GET
```kotlin
// Get a response data as a String
val text = CoHttp("https://example.net/api").toText()
```

### POST
```kotlin
// Post a form data
val body = "key=value".toByteArray()

val text = CoHttp("https://example.net/api")
    .method(Method.POST)
    .header("Content-Type", "application/x-www-form-urlencoded")
    .body(body)
    .toText()
```

## How to use `CoHttp` class

`CoHttp` class is a convenient utility to build an HTTP request and get a response data.

The class can build an HTTP request by method chain or `Request` interface.
Method chain is useful when you want to quickly write a request in an easy way.
Otherwise `Request` is useful when you want to parameterize part of a request and reuse it.

### Building a request by method chain
```kotlin
val responseText = CoHttp("https://example.net/api")
    .method(Method.POST)
    .headers("Content-Type" to "application/json")
    .queries("key1" to "value1")
    .body(body)
    .timeoutSeconds(20.0)
    .redirectEnabled(true)
    .toText()
```

### Building a request by Request interface
```kotlin
// Define a request
class GetUserRequest(val userId: String): Request {
    override val url: String get() = "https://example.net/api/user"

    override val method: Method get() = Method.POST

    override val headers: Map<String, String> get() = mapOf("Content-Type" to "application/json")

    override val body: ByteArray get() = "user=$userId".toByteArray()
}
```

```kotlin
// Use the request
val request = ExampleRequest(userId = "999")
val responseText = CoHttp(request).toText()
```

### Use response data

If you simply want the response body as a String or ByteArray, you can use the following methods of `CoHTTP` class.
- `suspend fun toText(charset: Charset? = null): String`
- `suspend fun toByteArray(): ByteArray`

If you want any custom object created from the raw response, you can use the following methods.

- `suspend fun <T> toModel(parse: (Response) -> T): T`
- `suspend fun <T> toModel(responseParser: ResponseParser<T>): T`

If you just want to see the raw response information such as status code or response headers, you can use this method.
- `suspend fun <T> useResponse(use: (Response) -> T): T`

All of these methods execute an HTTP communication and finally close the connection.

## How to use `API` interface

`API` interface consists of `Request` and `ResponseParser`, that can define an API specification as a class.
It can be executed and get a response model object as in the sample code written below.

```kotlin
// Define an API
class GetWeatherAPI<WeatherInfo>: API<WeatherInfo> {
    override val url: String get() = "https://example.net/api/weather"

    override fun parseResponse(response: Response): WeatherInfo {
        // Here we use Gson library for parsing JSON, but another JSON library can work.
        return Gson().fromJson(json, WeatherInfo::class.java)
    }
}
```

```kotlin
// Executes the API and gets a WeatherInfo object
val weatherInfo = GetWeatherAPI().execute()
```

You may think that creating one class per API is redundant, but it may make an application code more readable.

## HTTPException

In the following cases, an `HTTPException` is thrown.

- The request url was invalid.
- The network communication was failed.
- A custom validation method returned false.
- An exception occurred while parsing response data.
- The process was programmatically canceled.

## HTTPContext

`HTTPContext` is a configuration assumed to be shared by some HTTP communications.
When you execute HTTP communication, you can use your own `HTTPContext`, otherwise the default shared object is used.

`HTTPContext` can be set by constructors of `CoHttp` or an argument of `API.execute` function.
It is also possible to rewrite the properties of the default shared object.

This class has the following modules.
- a logger
- a cookie store
- a module to execute HTTP communication

You can replace these with any modules you provide.
Notably, by replacing the HTTP communication module, other HTTP libraries such as OkHttp can be used for the HTTP communication.

Because HTTP communication is complex and applications have varying requirements, we allow for this type of customization.