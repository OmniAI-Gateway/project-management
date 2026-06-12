package org.omniai.mcp.gateway.client

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

actual fun launchStdioProcess(command: String, args: List<String>): Pair<Source, Sink> {
    val fullCommand = listOf(command) + args
    val process = ProcessBuilder(fullCommand)
        .redirectErrorStream(false)
        .start()
    
    val source = process.inputStream.asSource().buffered()
    val sink = process.outputStream.asSink().buffered()
    
    return Pair(source, sink)
}
