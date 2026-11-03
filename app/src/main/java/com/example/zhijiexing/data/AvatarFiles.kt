package com.example.zhijiexing.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream

/** 头像图片的临时落盘、压缩与归档。 */
object AvatarFiles {
    private const val MAX_EDGE = 640
    private const val QUALITY = 88

    /** 拍照输出文件：放在 cache 里，交给 FileProvider 授权给相机应用。 */
    fun newCaptureFile(context: Context): File {
        val dir = File(context.cacheDir, "avatar_capture").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        return File(dir, "capture_${System.currentTimeMillis()}.jpg")
    }

    /** 读取相册/相机返回的图片，压到 640px 内的临时文件，供弹窗预览。 */
    fun preparePreview(context: Context, uri: Uri): String {
        val bitmap = decode(context, uri)
        val scaled = scaleDown(bitmap, MAX_EDGE)
        val dir = File(context.cacheDir, "avatar_preview").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "preview_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { scaled.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
        if (scaled !== bitmap) bitmap.recycle()
        scaled.recycle()
        return file.absolutePath
    }

    /** 确认保存后，把预览图归档到 files/avatars，并清掉该账号的旧头像。 */
    fun commit(context: Context, username: String, previewPath: String): String {
        val dir = File(context.filesDir, "avatars").apply { mkdirs() }
        val target = File(dir, "avatar_${username}_${System.currentTimeMillis()}.jpg")
        File(previewPath).copyTo(target, overwrite = true)
        val prefix = "avatar_${username}_"
        dir.listFiles()?.forEach { file ->
            if (file != target && file.name.startsWith(prefix)) file.delete()
        }
        return target.absolutePath
    }

    private fun decode(context: Context, uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                ?: throw IllegalStateException("无法读取图片")
        }
    }

    private fun scaleDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val max = maxOf(bitmap.width, bitmap.height)
        if (max <= maxEdge) return bitmap
        val ratio = maxEdge.toFloat() / max
        val width = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val height = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}
