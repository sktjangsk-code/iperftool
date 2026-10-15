package com.example.iperftool

import android.content.Context
import java.io.File

class IperfRunner(private val context: Context) {
    @Volatile
    private var process: Process? = null

    private fun binary(version: String): File {
        val name = if (version == "iPerf2") "libiperf2.so" else "libiperf3.so"
        return File(context.applicationInfo.nativeLibraryDir, name)
    }

    fun run(version: String, args: String, onLine: (String) -> Unit): Int {
        val bin = binary(version)
        if (!bin.exists()) {
            onLine("실행 파일이 없습니다: ${bin.name}")
            onLine("app/src/main/jniLibs/arm64-v8a/ 에 넣고 다시 빌드하세요.")
            return -1
        }
        val tokens = args.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.toMutableList()
        if (version != "iPerf2" && "--forceflush" !in tokens) tokens.add("--forceflush")

        val p = ProcessBuilder(listOf(bin.absolutePath) + tokens)
            .redirectErrorStream(true)
            .start()
        process = p
        p.inputStream.bufferedReader().forEachLine(onLine)
        val code = p.waitFor()
        process = null
        return code
    }

    fun stop() {
        process?.destroy()
    }
}
