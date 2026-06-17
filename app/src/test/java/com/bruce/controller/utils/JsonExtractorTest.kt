package com.bruce.controller.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JsonExtractorTest {

    @Test
    fun extractNextJson_completeJson_returnsJsonAndRemovesFromBuffer() {
        val buffer = StringBuilder("""{"key": "value"}""")
        val result = JsonExtractor.extractNextJson(buffer)

        assertEquals("""{"key": "value"}""", result)
        assertEquals("", buffer.toString())
    }

    @Test
    fun extractNextJson_garbageBeforeJson_returnsJsonAndRemovesGarbage() {
        val buffer = StringBuilder("""Some garbage data before {"key": "value"} and some after""")
        val result = JsonExtractor.extractNextJson(buffer)

        assertEquals("""{"key": "value"}""", result)
        assertEquals(" and some after", buffer.toString())
    }

    @Test
    fun extractNextJson_incompleteJson_returnsNullAndDoesNotModifyBuffer() {
        val buffer = StringBuilder("""{"key": "value"""") // Missing closing brace
        val result = JsonExtractor.extractNextJson(buffer)

        assertNull(result)
        assertEquals("""{"key": "value"""", buffer.toString())
    }

    @Test
    fun extractNextJson_noJson_returnsNull() {
        val buffer = StringBuilder("Just some plain text without any JSON")
        val result = JsonExtractor.extractNextJson(buffer)

        assertNull(result)
        assertEquals("Just some plain text without any JSON", buffer.toString())
    }

    @Test
    fun extractNextJson_noJsonLargeGarbage_clearsBuffer() {
        val buffer = StringBuilder()
        for (i in 0 until 5000) {
            buffer.append("a")
        }
        val result = JsonExtractor.extractNextJson(buffer)

        assertNull(result)
        assertEquals("", buffer.toString()) // Buffer should be cleared
    }

    @Test
    fun extractNextJson_escapedQuotes_handlesCorrectly() {
        val buffer = StringBuilder("""{"key": "value with \"escaped\" quotes"}""")
        val result = JsonExtractor.extractNextJson(buffer)

        assertEquals("""{"key": "value with \"escaped\" quotes"}""", result)
        assertEquals("", buffer.toString())
    }

    @Test
    fun extractNextJson_nestedObjects_handlesCorrectly() {
        val buffer = StringBuilder("""{"key": {"nestedKey": "nestedValue"}}""")
        val result = JsonExtractor.extractNextJson(buffer)

        assertEquals("""{"key": {"nestedKey": "nestedValue"}}""", result)
        assertEquals("", buffer.toString())
    }

    @Test
    fun extractNextJson_bracesInStrings_handlesCorrectly() {
        val buffer = StringBuilder("""{"key": "value with { and } inside"}""")
        val result = JsonExtractor.extractNextJson(buffer)

        assertEquals("""{"key": "value with { and } inside"}""", result)
        assertEquals("", buffer.toString())
    }
}
