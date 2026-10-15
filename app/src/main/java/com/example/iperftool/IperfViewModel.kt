package com.example.iperftool

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TpState(
    val streams: Map<String, Double> = emptyMap(),
    val liveSum: Double = 0.0,
    val elapsed: Int = 0,
    val omitting: Boolean = false,
    val finalSum: Double? = null
)

class IperfViewModel(app: Application) : AndroidViewModel(app) {
    private val runner = IperfRunner(app)
    private val store = HistoryStore(app)
    private var job: Job? = null
    private var userStopped = false
    private var parallel = 1
    private val sumHistory = mutableListOf<Double>()
    private var recvSum: Double? = null
    private var sendSum: Double? = null
    private val maxLogLines = 5000

    var version by mutableStateOf("iPerf3")
    var args by mutableStateOf("")
    var running by mutableStateOf(false)
        private set
    var tp by mutableStateOf(TpState())
        private set

    val logs = mutableStateListOf<String>()
    val history = mutableStateListOf<String>()

    init {
        history.addAll(store.load())
        args = history.firstOrNull() ?: "-c 172.1.195.254 -t 30 -i 1 -P 4 -O 3 -p 7001 -R"
    }

    fun start() {
        if (running) return
        val cmd = args.trim()
        if (cmd.isEmpty()) return

        history.clear()
        history.addAll(store.add(cmd))
        logs.clear()
        tp = TpState()
        sumHistory.clear()
        recvSum = null
        sendSum = null
        userStopped = false
        parallel = Regex("""-P\s*(\d+)""").find(cmd)?.groupValues?.get(1)?.toIntOrNull() ?: 1
        running = true

        job = viewModelScope.launch(Dispatchers.IO) {
            val code = try {
                runner.run(version, cmd) { line ->
                    viewModelScope.launch(Dispatchers.Main) { onLine(line) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { logs.add("실행 오류: ${e.message}") }
                -1
            }
            withContext(Dispatchers.Main) { finish(code) }
        }
    }

    fun stop() {
        userStopped = true
        runner.stop()
    }

    fun toggle() = if (running) stop() else start()

    private fun onLine(line: String) {
        if (logs.size >= maxLogLines) logs.removeRange(0, 500)
        logs.add(line)

        val p = IperfParser.parse(line) ?: return

        if (p.isSummary) {
            if (p.id == "SUM" || parallel == 1) {
                if (p.isReceiver) recvSum = p.mbps else sendSum = p.mbps
            }
            return
        }

        if (p.id == "SUM") {
            if (!p.omitted) sumHistory.add(p.mbps)
            tp = tp.copy(liveSum = p.mbps, elapsed = p.end.toInt(), omitting = p.omitted)
        } else {
            var next = tp.copy(streams = tp.streams + (p.id to p.mbps))
            if (parallel == 1) {
                if (!p.omitted) sumHistory.add(p.mbps)
                next = next.copy(liveSum = p.mbps, elapsed = p.end.toInt(), omitting = p.omitted)
            }
            tp = next
        }
    }

    private fun finish(code: Int) {
        running = false
        if (userStopped) {
            logs.add("--- 중지됨 ---")
            return
        }
        if (code != 0) logs.add("--- 종료 코드 $code ---")
        val avg = if (sumHistory.isNotEmpty()) sumHistory.average() else null
        tp = tp.copy(finalSum = recvSum ?: sendSum ?: avg)
    }

    fun selectHistory(cmd: String) {
        args = cmd
    }

    fun deleteHistory(cmd: String) {
        history.clear()
        history.addAll(store.remove(cmd))
    }

    fun clearHistory() {
        store.clear()
        history.clear()
    }
}
