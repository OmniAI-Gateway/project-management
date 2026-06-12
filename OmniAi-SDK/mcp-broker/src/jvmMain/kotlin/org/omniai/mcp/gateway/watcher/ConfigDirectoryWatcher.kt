package org.omniai.mcp.gateway.watcher

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import kotlin.concurrent.thread

actual class ConfigDirectoryWatcher actual constructor(private val directoryPath: String) {

    private var watchingThread: Thread? = null
    @Volatile
    private var isRunning = false

    actual fun startWatching(onChange: (List<String>) -> Unit) {
        if (isRunning) return
        isRunning = true

        val dirPath: Path = Paths.get(directoryPath)
        val file = dirPath.toFile()
        if (!file.exists()) {
            file.mkdirs()
        }

        watchingThread = thread(start = true, isDaemon = true, name = "ConfigDirectoryWatcher") {
            val watchService = FileSystems.getDefault().newWatchService()
            dirPath.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
            )

            while (isRunning) {
                val key: WatchKey
                try {
                    key = watchService.take()
                } catch (e: InterruptedException) {
                    break
                }

                var changed = false
                for (event in key.pollEvents()) {
                    val kind = event.kind()
                    if (kind == StandardWatchEventKinds.OVERFLOW) continue
                    
                    val changedPath = event.context() as Path
                    if (changedPath.toString().endsWith(".yaml") || changedPath.toString().endsWith(".yml")) {
                        changed = true
                    }
                }

                val valid = key.reset()
                if (changed) {
                    try {
                        val contents = readAllYamlFiles()
                        onChange(contents)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (!valid) break
            }
            watchService.close()
        }
    }

    actual fun stopWatching() {
        isRunning = false
        watchingThread?.interrupt()
        watchingThread = null
    }

    actual fun readAllYamlFiles(): List<String> {
        val dir = File(directoryPath)
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        return dir.listFiles { file -> 
            file.isFile && (file.name.endsWith(".yaml") || file.name.endsWith(".yml"))
        }?.map { it.readText() } ?: emptyList()
    }
}
