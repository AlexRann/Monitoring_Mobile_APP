package com.example.wewatchyou

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import com.example.wewatchyou.databinding.ActivityMainBinding
import com.example.wewatchyou.global.globalImg
import com.example.wewatchyou.global.globalPermCam
import kotlinx.android.synthetic.main.fragment_second.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    // Camera
    private var imageCapture : ImageCapture? = null
    private lateinit var cameraExecutor : ExecutorService



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        //setupActionBarWithNavController(navController, appBarConfiguration)


        // Check camera permissions if all permission granted start camera else ask for the permission
        if (allPermissionsGranted()) {
            globalPermCam = true
            //startCamera()
        } else {
            globalPermCam = false
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        //cameraExecutor = Executors.newSingleThreadExecutor()
        //setUpSensorStuff ()
    }





    /* ========================================================================================================================
       ========================================================================================================================
       =================================================== CAMERA =============================================================
       =================================================== MAIN V ============================================================= */

    // https://fr.acervolima.com/comment-creer-une-camera-personnalisee-a-l-aide-de-camerax-sous-android/
    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(cameraExecutor, object :
            ImageCapture.OnImageCapturedCallback() {

            // https://stackoverflow.com/questions/70164601/android-camerax-image-capture-onimagesaved-never-runs
            override fun onCaptureSuccess(image: ImageProxy) {
                // From ImageProxy To Bitmap
                val bitmap = imageProxyToBitmap(image)
                // From Bitmap to String Base64
                val imageStr = encodeImage( rotateImage(bitmap, 270f)!! )
                globalImg = imageStr.toString()
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
            }
        })
    }



    // https://stackoverflow.com/questions/14066038/why-does-an-image-captured-using-camera-intent-gets-rotated-on-some-devices-on-a
    fun rotateImage(source: Bitmap, angle: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(angle)

        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }



    // Image au format ImageProxy à Bitmap
    private fun imageProxyToBitmap (image: ImageProxy): Bitmap {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }



    // Image au format Bitmap à String Base64
    private fun encodeImage(bm: Bitmap): String? {
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 60, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.NO_WRAP)
    }



    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder2.createSurfaceProvider())
                }

            imageCapture = ImageCapture.Builder().build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

                imageCapture!!.takePicture(cameraExecutor, object :
                    ImageCapture.OnImageCapturedCallback() {

                    // https://stackoverflow.com/questions/70164601/android-camerax-image-capture-onimagesaved-never-runs
                    override fun onCaptureSuccess(image: ImageProxy) {
                        // From ImageProxy To Bitmap
                        val bitmap = imageProxyToBitmap(image)
                        // From Bitmap to String Base64
                        val imageStr = encodeImage( rotateImage(bitmap, 270f)!! )
                        globalImg = imageStr.toString()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        super.onError(exception)
                    }
                })

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }



    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }



    // Checks the camera permission
    override fun onRequestPermissionsResult( requestCode: Int, permissions: Array<String>, grantResults: IntArray ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // If all permissions granted , then start Camera
            if (allPermissionsGranted()) {
                //startCamera()
                globalPermCam = true
            } else {
                globalPermCam = false
                // If permissions are not granted, present a toast to notify the user that the permissions were not granted.
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                //finish()
            }
        }
    }



    companion object {
        private const val TAG = ">>> Camera"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

/* ======================================================= MAIN V =========================================================
   ======================================================= CAMERA =========================================================
   ========================================================================================================================
   ======================================================================================================================== */

    // https://stackoverflow.com/questions/8631095/how-to-prevent-going-back-to-the-previous-activity
    override fun onBackPressed() {
        Toast.makeText(this, "Impossible de retourner en arrière", Toast.LENGTH_SHORT).show()
    }
}