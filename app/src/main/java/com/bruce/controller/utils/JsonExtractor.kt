package com.bruce.controller.utils

object JsonExtractor {

    /**
     * Extracts the first balanced JSON object from the given [buffer].
     * If found, the JSON string is returned and removed from the buffer (along with any preceding garbage).
     * If not found, returns null. If the buffer has accumulated too much garbage without a JSON object, it clears it.
     */
    fun extractNextJson(buffer: StringBuilder): String? {
        val text = buffer.toString()
        val start = text.indexOf('{')
        if (start < 0) {
            // garbage without JSON — sometimes can be cleared if too much accumulated
            if (buffer.length > 4000) {
                buffer.clear()
            }
            return null
        }
        var depth = 0
        var inString = false
        var escape = false
        var foundEnd = -1
        for (i in start until text.length) {
            val c = text[i]
            if (escape) { escape = false; continue }
            when {
                c == '\\' && inString -> escape = true
                c == '"' -> inString = !inString
                !inString && c == '{' -> depth++
                !inString && c == '}' -> {
                    depth--
                    if (depth == 0) { foundEnd = i; break }
                }
            }
        }
        if (foundEnd < 0) return null // JSON not yet fully read — wait

        val json = text.substring(start, foundEnd + 1)
        buffer.delete(0, foundEnd + 1)
        return json
    }
}
