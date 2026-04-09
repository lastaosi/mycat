package com.lastaosi.mycat.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File

/**
 * 갤러리에서 선택한 이미지를 앱 내부 저장소에 저장하고 파일 경로를 반환한다.
 *
 * ## 처리 순서
 * 1. URI → InputStream 으로 Bitmap 디코딩
 * 2. **스트림을 다시 열어** EXIF orientation 읽기
 *    (BitmapFactory.decodeStream 은 스트림을 소비하므로 재사용 불가 → 두 번 열어야 함)
 * 3. EXIF orientation 에 맞게 Bitmap 회전 ([rotateBitmap])
 * 4. 최대 800px 로 리사이즈 ([resizeBitmap]) → Gemini API 전송 및 저장 용량 최적화
 * 5. JPEG 80% 품질로 압축해 filesDir 에 저장
 *
 * @return 저장된 파일의 절대경로. 실패 시 null.
 */
fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val fileName = "cat_${System.currentTimeMillis()}.jpg"
        val destFile = File(context.filesDir, fileName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            val bitmap = BitmapFactory.decodeStream(input)

            // BitmapFactory.decodeStream 이 스트림을 모두 읽어버리므로
            // EXIF 정보를 읽으려면 스트림을 다시 열어야 한다.
            val rotation = context.contentResolver.openInputStream(uri)?.use { exifInput ->
                val exif = ExifInterface(exifInput)
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL

            val rotated = rotateBitmap(bitmap, rotation)
            val resized = resizeBitmap(rotated, 800)

            destFile.outputStream().use { output ->
                resized.compress(Bitmap.CompressFormat.JPEG, 80, output)
            }
        }
        destFile.absolutePath
    } catch (e: Exception) {
        null
    }
}

/**
 * EXIF orientation 값에 따라 비트맵 회전
 */
private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90  -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL   -> matrix.preScale(1f, -1f)
        else -> return bitmap  // NORMAL이면 그대로
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

/**
 * 가로 또는 세로 중 긴 변이 [maxSize] 를 넘지 않도록 비율을 유지하며 리사이즈한다.
 * 이미 [maxSize] 이하이면 원본을 그대로 반환한다.
 */
private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= maxSize && height <= maxSize) return bitmap

    // 가로세로 비율 유지: 긴 변을 maxSize 에 맞추고 짧은 변은 비율에 따라 결정
    val ratio = width.toFloat() / height.toFloat()
    val newWidth: Int
    val newHeight: Int
    if (width > height) {
        newWidth = maxSize
        newHeight = (maxSize / ratio).toInt()
    } else {
        newHeight = maxSize
        newWidth = (maxSize * ratio).toInt()
    }
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}