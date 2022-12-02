package cohttp

import kotlinx.coroutines.runBlocking
import org.junit.Test

class CoHttpTests {
    @Test
    fun printGetText() = runBlocking {
        val text = CoHttp("https://github.com/").toText()
        println(text)
    }
}