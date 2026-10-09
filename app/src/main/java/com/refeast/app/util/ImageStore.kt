package com.refeast.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import com.refeast.app.R
import java.io.File
import java.io.FileOutputStream

/** Saves food photos inside the app's own storage and shows them in an ImageView. */
object ImageStore {

    /** Copies the picked photo into the app's folder and returns the file path (or null if it failed). */
    fun save(context: Context, uri: Uri): String? {
        try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            // inSampleSize = 4 makes the photo 4 times smaller so it does not use too much memory.
            val options = BitmapFactory.Options()
            options.inSampleSize = 4
            val bitmap = BitmapFactory.decodeStream(input, null, options)
            input.close()
            if (bitmap == null) return null

            val folder = File(context.filesDir, "images")
            folder.mkdirs()
            val file = File(folder, "food_" + System.currentTimeMillis() + ".jpg")
            val output = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
            output.close()
            return file.absolutePath
        } catch (e: Exception) {
            return null
        }
    }

    /** Shows the photo if there is one, otherwise the drawing for the food category. */
    fun show(imageView: ImageView, imagePath: String?, category: String?) {
        if (imagePath != null && File(imagePath).exists()) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                return
            }
        }
        imageView.setImageResource(illustrationFor(category))
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
    }

    /** Built-in drawing for each food category. */
    fun illustrationFor(category: String?): Int {
        return when (category) {
            "Cooked Meals" -> R.drawable.food_cooked
            "Fresh Produce" -> R.drawable.food_produce
            "Baked Goods" -> R.drawable.food_baked
            "Dry / Pantry" -> R.drawable.food_pantry
            "Beverages" -> R.drawable.food_beverage
            else -> R.drawable.ic_food
        }
    }
}
