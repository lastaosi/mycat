package com.lastaosi.mycat.data.remote

import platform.Foundation.NSData
import platform.Foundation.create
import platform.Foundation.base64EncodedStringWithOptions

actual fun encodeBase64(bytes: ByteArray): String {
    val data = NSData.create(bytes = bytes, length = bytes.size.toULong())
    return data.base64EncodedStringWithOptions(0u)
}