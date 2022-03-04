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
    // ======================================= DEBUT, REPRISE DU CODE =======================================
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }



    // Checks the camera permission
    override fun onRequestPermissionsResult( requestCode: Int, permissions: Array<String>, grantResults: IntArray ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // If all permissions granted , then start Camera
// ============================== DEBUT, MODIFICATION APPORTEE ==============================
            if (allPermissionsGranted()) {
                //startCamera()
                globalPermCam = true
            } else {
                globalPermCam = false
// ============================== FIN, MODIFICATION APPORTEE ==============================
                // If permissions are not granted, present a toast to notify the user that the permissions were not granted.
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                //finish()
            }
        }
    }



    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
    // ======================================= FIN, REPRISE DU CODE =======================================

/* ======================================================= MAIN V =========================================================
   ======================================================= CAMERA =========================================================
   ========================================================================================================================
   ======================================================================================================================== */

    // https://stackoverflow.com/questions/8631095/how-to-prevent-going-back-to-the-previous-activity
    override fun onBackPressed() {
        Toast.makeText(this, "Impossible de retourner en arrière", Toast.LENGTH_SHORT).show()
    }
}