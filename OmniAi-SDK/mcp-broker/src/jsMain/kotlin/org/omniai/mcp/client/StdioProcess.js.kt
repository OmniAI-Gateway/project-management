package org.omniai.mcp.client

import kotlinx.io.Sink
import kotlinx.io.Source

actual fun launchStdioProcess(
    command: String,
    args: List<String>,
): Pair<Source, Sink> {
    // For JS/Node, a child process must be spawned and wrapped in kotlinx.io Source/Sink.
    // For now, throw an unsupported operation until Node.js child_process integration is fully needed.
    throw UnsupportedOperationException("STDIO process launching is not yet implemented for JS target")
}
