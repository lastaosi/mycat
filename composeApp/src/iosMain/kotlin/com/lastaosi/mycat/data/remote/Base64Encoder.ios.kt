package com.lastaosi.mycat.data.remote

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Foundation.base64EncodedStringWithOptions

@OptIn(ExperimentalForeignApi::class)
actual fun encodeBase64(bytes: ByteArray): String {
    val data = bytes.usePinned { pinned ->
        NSData.create(
            bytes = pinned.addressOf(0),
            length = bytes.size.toULong()
        )
    }
    return data.base64EncodedStringWithOptions(0u)
}