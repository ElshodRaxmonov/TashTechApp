package com.mrdev.tashtech.tashbox.network

import java.io.BufferedReader
import java.io.IOException

internal object LineReader {
    /** Reads one '\n'-terminated line; null at end of stream. Rejects absurdly long lines. */
    fun readLine(reader: BufferedReader, maxChars: Int = Protocol.MAX_LINE_CHARS): String? {
        val sb = StringBuilder()
        while (true) {
            val ch = reader.read()
            if (ch == -1) return if (sb.isEmpty()) null else sb.toString()
            if (ch == '\n'.code) return sb.toString()
            if (ch != '\r'.code) {
                sb.append(ch.toChar())
                if (sb.length > maxChars) throw IOException("Line too long")
            }
        }
    }
}
