package com.lastaosi.mycat.data.remote

import android.util.Base64

actual fun encodeBase64(bytes: ByteArray): String =
    Base64.encodeToString(bytes, Base64.NO_WRAP)