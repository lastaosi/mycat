package com.lastaosi.mycat.util

actual fun getCallerInfo(): String {
    return try {
        val stackTrace = Thread.currentThread().stackTrace
        val callerFrame = stackTrace.firstOrNull { frame ->
            !frame.className.contains("com.lastaosi.mycat.util.L") &&
                    !frame.className.contains("com.lastaosi.mycat.util.CallerInfo") &&
                    !frame.className.contains("CallerInfo_androidKt") &&
                    !frame.className.contains("dalvik.system") &&
                    !frame.className.contains("java.lang.Thread") &&
                    !frame.className.contains("java.lang.reflect") &&
                    frame.className.isNotEmpty()
        }
        callerFrame?.let {
            val simpleClassName = it.className
                .substringAfterLast(".")
                .removeSuffix("Kt")  // 코틀린 파일 클래스명 정리
            "(${simpleClassName}.kt:${it.lineNumber}) ${it.methodName}()"
        } ?: "unknown"
    } catch (e: Exception) {
        "unknown"
    }
}