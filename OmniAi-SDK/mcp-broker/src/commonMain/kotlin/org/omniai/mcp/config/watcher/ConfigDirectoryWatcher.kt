package org.omniai.mcp.config.watcher

/**
 * Multiplatform abstraction for watching a directory for YAML configuration changes.
 */
expect class ConfigDirectoryWatcher(
    directoryPath: String,
) {
    /**
     * Start watching the directory. When a file is added, modified, or deleted,
     * the [onChange] callback is invoked with the current content of all YAML files
     * in the directory.
     */
    fun startWatching(onChange: (List<String>) -> Unit)

    /**
     * Stop watching the directory.
     */
    fun stopWatching()

    /**
     * Reads all YAML files in the directory synchronously and returns their contents.
     */
    fun readAllYamlFiles(): List<String>
}
