package org.omniai.mcp.config.watcher

actual class ConfigDirectoryWatcher actual constructor(
    private val directoryPath: String,
) {
    private val fs: dynamic = js("require('fs')")
    private val pathModule: dynamic = js("require('path')")
    private var watcher: dynamic = null

    actual fun startWatching(onChange: (List<String>) -> Unit) {
        if (watcher != null) return

        if (!(fs.existsSync(directoryPath) as Boolean)) {
            fs.mkdirSync(directoryPath, js("{ recursive: true }"))
        }

        watcher =
            fs.watch(directoryPath) { _: String, filename: String? ->
                if (filename != null && (filename.endsWith(".yaml") || filename.endsWith(".yml"))) {
                    try {
                        val contents = readAllYamlFiles()
                        onChange(contents)
                    } catch (e: Exception) {
                        console.error("Error reading yaml files", e)
                    }
                }
            }
    }

    actual fun stopWatching() {
        if (watcher != null) {
            watcher.close()
            watcher = null
        }
    }

    actual fun readAllYamlFiles(): List<String> {
        if (!(fs.existsSync(directoryPath) as Boolean)) return emptyList()

        val files: Array<String> = fs.readdirSync(directoryPath) as Array<String>
        val yamlContents = mutableListOf<String>()

        for (file in files) {
            if (file.endsWith(".yaml") || file.endsWith(".yml")) {
                val fullPath = pathModule.join(directoryPath, file)
                val stat = fs.statSync(fullPath)
                if (stat.isFile() as Boolean) {
                    val content = fs.readFileSync(fullPath, "utf8") as String
                    yamlContents.add(content)
                }
            }
        }

        return yamlContents
    }
}
