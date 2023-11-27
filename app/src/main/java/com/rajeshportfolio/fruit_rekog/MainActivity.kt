package com.rajeshportfolio.fruit_rekog

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.rajeshportfolio.fruit_rekog.ml.Model
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    lateinit var imageShow : ImageView
    lateinit var camera : Button
    lateinit var gallery : Button
    lateinit var classify : TextView
    var imageSize = 32;

    override fun onCreate(savedInstanceState: Bundle?) {
//        create a imageShow and assignit a default value
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        camera = findViewById(R.id.camera)
        gallery = findViewById(R.id.gallery)
        imageShow = findViewById(R.id.imageView)
        classify = findViewById(R.id.classify)

//        create a camera click listener
        camera.setOnClickListener {
            if(checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED){
                requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 100)
            }
            else{
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, 100)
            }
        }
//        create a gallery click listener
        gallery.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK)
            galleryIntent.type = "image/*"
            startActivityForResult(galleryIntent, 3)
        }

    }

    fun classifyImage(bitmap: Bitmap) {
       try {
           val model : Model = Model.newInstance(this)
           val inputFeature0 : TensorBuffer = TensorBuffer.createFixedSize(intArrayOf(1, imageSize, imageSize, 3), org.tensorflow.lite.DataType.FLOAT32)
           val byteBuffer : ByteBuffer = ByteBuffer.allocateDirect(imageSize * imageSize * 3 * 4)

           byteBuffer.order(ByteOrder.nativeOrder())
              val intValues = IntArray(imageSize * imageSize)
                bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                var pixel = 0
                for (i in 0 until imageSize) {
                    for (j in 0 until imageSize) {
                        val `val` = intValues[pixel++]
                        byteBuffer.putFloat((`val` shr 16 and 0xFF) / 1f)
                        byteBuffer.putFloat((`val` shr 8 and 0xFF) / 1f)
                        byteBuffer.putFloat((`val` and 0xFF) / 1f)
                    }
                }

              inputFeature0.loadBuffer(byteBuffer)

          val outputs = model.process(inputFeature0)
          val outputFeature0 = outputs.outputFeature0AsTensorBuffer
          val confidences = outputFeature0.floatArray.contentToString()
          var maxPos = 0;
          var maxConfidence = 0;

           Toast.makeText(this, confidences, Toast.LENGTH_SHORT).show()

//           get the confidences length
              val confidencesLength = outputFeature0.floatArray.size
//           loop through the confidences
                for (i in 0 until confidencesLength) {
                    if (outputFeature0.floatArray[i] > maxConfidence) {
                        maxConfidence = outputFeature0.floatArray[i].toInt()
                        maxPos = i
                    }
                }

           Toast.makeText(this, maxPos.toString(), Toast.LENGTH_SHORT).show()

           classify.text = "Predicted Fruit is: " + getFruitName(maxPos)
           model.close()
            } catch (e: Exception) {
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
            }
       }

    private fun getFruitName(max: Any): Any? {
        return when (max) {
            0 -> "Apple"
            1 -> "Banana"
            2 -> "orange"
            else -> "Not Found"
        }
    }

    private fun getMax(floatArray: FloatArray?): Any {
        var ind = 0
        var min = 0.0f
        for (i in 0..9) {
            if (floatArray!![i] > min) {
                min = floatArray[i]
                ind = i
            }
        }
        return ind
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == RESULT_OK) { // Check if the result is from the camera request

            var imageBitmap : Bitmap = data?.extras?.get("data") as Bitmap;
            val dimension = Math.min(imageBitmap.getWidth(), imageBitmap.getHeight());
            val image = ThumbnailUtils.extractThumbnail(imageBitmap, dimension, dimension);
            imageShow.setImageBitmap(image)
            imageBitmap = Bitmap.createScaledBitmap(imageBitmap, imageSize, imageSize, true)
            classifyImage(imageBitmap)

        } else if (requestCode == 3 && resultCode == RESULT_OK) {
            if (data != null) {
                val contentURI = data.data
                val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, contentURI)
                imageShow.setImageBitmap(bitmap)
                val dimension = Math.min(bitmap.getWidth(), bitmap.getHeight());
                val image = ThumbnailUtils.extractThumbnail(bitmap, dimension, dimension);
                imageShow.setImageBitmap(image)
                val imageBitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true)
                classifyImage(imageBitmap)
            } else {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
