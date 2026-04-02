package com.lastaosi.mycat.util

object L {

    private const val PRINT_LOG = true
    private const val TAG = "MYCAT_LOG"

    fun v(log: String?) = print("VERBOSE", log)
    fun i(log: String?) = print("INFO", log)
    fun d(log: String?) = print("DEBUG", log)
    fun w(log: String?) = print("WARN", log)
    fun e(log: String?) = print("ERROR", log)
    fun e(e: Throwable) = print("ERROR", e.stackTraceToString())

    fun d(log: Any?) = print("DEBUG", log?.toString())

    fun d(vararg items: Pair<String, Any?>) {
        if (!PRINT_LOG) return
        val caller = getCallerInfo()
        val log = items.joinToString("\n") { "${it.first} : ${it.second}" }
        println("[$TAG/DEBUG] $caller\n$log")
    }

    fun d(titles: List<String>, contents: List<Any?>) {
        if (!PRINT_LOG) return
        val caller = getCallerInfo()
        val log = titles.zip(contents) { title, content ->
            "$title : $content"
        }.joinToString("\n")
        println("[$TAG/DEBUG] $caller\n$log")
    }

    fun d(items: List<String?>) {
        if (!PRINT_LOG) return
        println("[$TAG/DEBUG] ${getCallerInfo()}\n${items.joinToString("\n")}")
    }

    fun d(vararg items: String) {
        if (!PRINT_LOG) return
        println("[$TAG/DEBUG] ${getCallerInfo()}\n${items.joinToString("\n")}")
    }

    private fun print(level: String, log: String?) {
        if (!PRINT_LOG) return
        println("[$TAG/$level] ${getCallerInfo()} | ${log ?: "null"}")
    }
}

// expect 선언
expect fun getCallerInfo(): String