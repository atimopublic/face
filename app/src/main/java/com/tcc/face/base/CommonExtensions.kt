package com.tcc.face.base

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Base64
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.util.UUID

fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun View.showKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, 0)
}

fun TextView.makeLinks(vararg links: Pair<Int, (View) -> (Unit)>) {
    val spannableString = SpannableString(this.text)
    var startIndexOfLink = -1
    for (link in links) {
        val clickableSpan = object : ClickableSpan() {
            override fun updateDrawState(textPaint: TextPaint) {
                // use this to change the link color
//                textPaint.color = textPaint.linkColor
                // toggle below value to enable/disable
                // the underline shown below the clickable text
                textPaint.isUnderlineText = true
            }

            override fun onClick(view: View) {
                Selection.setSelection((view as TextView).text as Spannable, 0)
                view.invalidate()
                link.second(view)
            }
        }
        val linkText = resources.getText(link.first).toString()
        startIndexOfLink = this.text.toString().indexOf(linkText, startIndexOfLink + 1)
//      if(startIndexOfLink == -1) continue // todo if you want to verify your texts contains links text
        spannableString.setSpan(
            clickableSpan, startIndexOfLink, startIndexOfLink + linkText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
    this.movementMethod = LinkMovementMethod.getInstance() // without LinkMovementMethod, link can not click
    this.setText(spannableString, TextView.BufferType.SPANNABLE)
}

fun Long.toTimeStr(): String {
    val sec = this/1000
    val minStr = if (sec/60 < 10) "0${sec/60}" else "${sec/60}"
    val secStr = if (sec%60 < 10) "0${sec%60}" else "${sec%60}"
    return "$minStr:$secStr"
}

val Int.px: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Float.dp: Float
    get() = this * Resources.getSystem().displayMetrics.density

fun String.base64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
fun ByteArray.base64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
fun Bitmap.base64Jpeg(): String {
    val bitmapBytes = ByteArrayOutputStream().apply {
        compress(Bitmap.CompressFormat.JPEG, 100, this)
    }.toByteArray()

    return bitmapBytes.base64()

}

fun File.base64(): String {
    return if (this.exists()) {
        readBytes().base64()
    } else {
        ""
    }
}

fun File.cleanDirectory() {
    if (isDirectory) {
        val files = listFiles()
        files?.let {
            for (file in it) {
                if (file.isDirectory) {
                    file.cleanDirectory()
                } else {
                    file.delete()
                }
            }
        }
    }
}

fun File.saveBitmap(bitmap: Bitmap) {
    parentFile.mkdirs()
    FileOutputStream(this).apply {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, this)
    }
}

fun File.resizeImage(maxSize: Int) {
    saveBitmap(decodeBitmap(maxSize))
}

fun File.jpegCompress(quality: Int, maxSize: Int = 0) {
    val bitmap = decodeBitmap(maxSize)
    FileOutputStream(this).apply {
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, this)
    }
}

fun File.decodeBitmap(size: Int = 0): Bitmap {
    if (!this.exists()) {
        return Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
    }
    val options = BitmapFactory.Options()

    if (size != 0) {
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(absolutePath, options)

        options.inSampleSize = calculateInSampleSize(options, size, size)
        options.inJustDecodeBounds = false
    }

    val photo = BitmapFactory.decodeFile(absolutePath, options)
  //  XLogger.i("DecodeBitmap is (photo == null) : ${photo == null}")
    return photo.rotatePhotoIfNeeded(this)
}

fun Bitmap.rotateImage(degree: Float): Bitmap {
    return if (degree == 0f) {
        this
    } else {
        val matrix = Matrix()
        matrix.postRotate(degree)

        Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
}

fun Bitmap.rotate(degree: Float, pivot: PointF): Bitmap {
    val matrix = Matrix().apply {
        setRotate(degree, pivot.x, pivot.y)
    }
    val newBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    android.graphics.Canvas(newBmp).apply {
        drawBitmap(this@rotate, matrix, Paint().apply { isFilterBitmap = true })
//        drawBitmap(this@rotate, matrix, Paint().apply { isAntiAlias = true })
    }
    recycle()

    return newBmp
}


fun Bitmap.rotatePhotoIfNeeded(ei: ExifInterface): Bitmap {
    val orientation: Int = ei.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_UNDEFINED
    )

    return when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(270f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> flipImageVertical()
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flipImageHorizontal()
        ExifInterface.ORIENTATION_TRANSPOSE -> flipImageHorizontal().rotateImage(270f)
        ExifInterface.ORIENTATION_TRANSVERSE -> flipImageHorizontal().rotateImage(90f)
        else -> this
    }
}

fun Bitmap.rotatePhotoIfNeeded(photoFile: File): Bitmap {
    val ei = ExifInterface(photoFile)
    return rotatePhotoIfNeeded(ei)
}

fun Bitmap.flipImageVertical() = flipImage(ExifInterface.ORIENTATION_FLIP_VERTICAL)
fun Bitmap.flipImageHorizontal() = flipImage(ExifInterface.ORIENTATION_FLIP_HORIZONTAL)

private fun Bitmap.flipImage(direction: Int): Bitmap {
    val matrix = Matrix()
    when (direction) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(
            -1f,
            1f,
            width / 2f,
            height / 2f
        )
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(
            1f,
            -1f,
            width / 2f,
            height / 2f
        )
    }

    val rotatedImg = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    recycle()

    return rotatedImg
}

fun ByteArray.saveToNewFile(dir: File): File {
    dir.mkdirs()
    val file = File(dir, "${UUID.randomUUID()}.jpg")
    file.writeBytes(this)

    return file
}

fun String?.base64Bitmap(): Bitmap? {
    if (this == null) return null

    val bytes = base64()
    val inputStream = ByteArrayInputStream(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    return try {
        val ei = ExifInterface(inputStream)
        bitmap.rotatePhotoIfNeeded(ei)
    } catch (ex: Exception) {
       // XLogger.e(ex)
        return bitmap
    }
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {

        while (height / inSampleSize >= reqHeight && width / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}

fun Context.getBitmapFromUri(uri: Uri): Bitmap {
    val parcelFileDescriptor: ParcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")!!
    val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor
    val image: Bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
    val exif = ExifInterface(fileDescriptor)
    parcelFileDescriptor.close()
    return image.rotatePhotoIfNeeded(exif)
}

fun isImageUri(context: Context, uri: Uri): Boolean {
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(uri)
    return mimeType?.startsWith("image/") == true
}
fun Uri.decodeBitmap(context: Context, size: Int = 0): Bitmap {
    val parcelFileDescriptor: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(this, "r")!!
    val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor

    val options = BitmapFactory.Options()
    if (size != 0) {
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFileDescriptor(fileDescriptor, Rect(), options)

        options.inSampleSize = calculateInSampleSize(options, size, size)
        options.inJustDecodeBounds = false
    }

    val photo: Bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, Rect(), options)
    val exif = ExifInterface(fileDescriptor)
    parcelFileDescriptor.close()

    return photo.rotatePhotoIfNeeded(exif)
}

fun Uri.decodeFileData(context: Context): ByteArray {
    val inStream = context.contentResolver.openInputStream(this)
    val data = inStream?.readBytes() ?: ByteArray(0)
    inStream?.close()
    return data
}

fun Long.timeToHHmmssString(): String {
    var remainder = this
    val min = this/(60*1000)
    remainder -= min*60*1000
    val sec = remainder/1000
    remainder -= sec*1000
    val ms = remainder/10

    return "${min.toString().padStart(2, '0')}:" +
            "${sec.toString().padStart(2, '0')}:" +
            ms.toString().padStart(2, '0')
}

fun Fragment.getNavigationResult(key: String = "result") =
    findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>(key)

fun <T> Fragment.setNavigationResult(result: T, key: String = "result") {
    findNavController().previousBackStackEntry?.savedStateHandle?.set(key, result)
}