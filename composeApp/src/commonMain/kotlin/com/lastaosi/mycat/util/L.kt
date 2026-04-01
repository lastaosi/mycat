package com.lastaosi.mycat.util

object L {

    private const val PRINT_LOG = true
    private const val TAG = "MYCAT_LOG"

    fun v(log: String?) = print("VERBOSE", log)
    fun i(log: String?) = print("INFO", log)
    fun d(log: String?) = print("DEBUG", log)
    fun w(log: String?) = print("WARN", log)
    fun e(log: String?) = print("ERROR", log)
    fun e(e: Throwable) = print("ERROR", e.toString())

    // 객체를 toString()으로 출력
    fun d(log: Any?) = print("DEBUG", log?.toString())

    // Pair 목록 출력
    fun d(vararg items: Pair<String, Any?>) {
        if (!PRINT_LOG) return
        val log = items.joinToString("\n") { "${it.first} : ${it.second}" }
        println("[$TAG/DEBUG]\n$log")
    }

    // 제목-값 쌍 출력
    fun d(titles: List<String>, contents: List<Any?>) {
        if (!PRINT_LOG) return
        val log = titles.zip(contents) { title, content ->
            "$title : $content"
        }.joinToString("\n")
        println("[$TAG/DEBUG]\n$log")
    }

    fun d(items: List<String?>) {
        if (!PRINT_LOG) return
        println("[$TAG/DEBUG]\n${items.joinToString("\n")}")
    }

    fun d(vararg items: String) {
        if (!PRINT_LOG) return
        println("[$TAG/DEBUG]\n${items.joinToString("\n")}")
    }

    private fun print(level: String, log: String?) {
        if (!PRINT_LOG) return
        println("[$TAG/$level] ${log ?: "null"}")
    }
}