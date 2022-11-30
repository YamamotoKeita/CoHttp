package cohttp.model

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Query parameters of URL
 */
class URLQuery {

    var keyValues: MutableList<KeyValue> = mutableListOf()

    /**
     * Generally query parameters are encoded with URL-Encode of UTF-8, but in some special cases, another charset may be used.
     */
    var encoder = defaultEncoder

    constructor()

    constructor(string: String, encoder: Encoder = defaultEncoder) {
        keyValues = string.components(separatedBy = "&").filter { it.isNotEmpty() }.map {
            val pairs = it.components(separatedBy = "=")
            val key = encoder.decode(pairs[0])
            val value = if (1 < pairs.size) encoder.decode(pairs[1]) else null
            KeyValue(key = key, value = value)
        }.toMutableList()
    }

    constructor(vararg elements: Pair<String, String?>) {
        keyValues = elements.map { KeyValue(key = it.first, value = it.second) }.toMutableList()
    }

    constructor(map: Map<String, String?>) {
        keyValues = map.map {
            KeyValue(it.key, it.value)
        }.toMutableList()
    }

    constructor(keyValues: List<KeyValue>) {
        this.keyValues = keyValues.toMutableList()
    }

    operator fun get(key: String): String? {
        return keyValues.firstOrNull { it.key == key }?.value
    }

    operator fun set(key: String, value: String?) {
        val keyValue = KeyValue(key = key, value = value)
        val offset = keyValues.indexOfFirst { it.key == key }
        if (offset != -1) {
            keyValues[offset] = keyValue
        } else {
            keyValues.add(keyValue)
        }
    }

    fun isEmpty(): Boolean = keyValues.isEmpty()

    fun queryString(): String {
        return keyValues.joinToString(separator = "&") {
            it.encodedValue(encoder::encode)
        }
    }

    private fun String.components(separatedBy: String): List<String> {
        return split(separatedBy)
    }

    data class KeyValue(val key: String, val value: String?) {

        fun encodedValue(encodeValue: (String) -> String): String {
            var item = encodeValue(key)
            val value = value
            if (value != null) {
                item += "=${encodeValue(value)}"
            }
            return item
        }
    }

    companion object {
        /**
         * Default implementation of URL encode/decode
         */
        val defaultEncoder: Encoder = object : Encoder {
            override fun encode(value: String): String {
                return URLEncoder.encode(value, "UTF-8")
            }

            override fun decode(value: String): String {
                return URLDecoder.decode(value, "UTF-8")
            }
        }
    }

    interface Encoder {
        fun encode(value: String): String
        fun decode(value: String): String
    }
}