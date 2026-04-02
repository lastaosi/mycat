package com.lastaosi.mycat.data.remote

import platform.Foundation.NSBundle

actual fun getGeminiApiKey(): String =
    NSBundle.mainBundle.objectForInfoDictionaryKey("GEMINI_API_KEY") as? String ?: ""