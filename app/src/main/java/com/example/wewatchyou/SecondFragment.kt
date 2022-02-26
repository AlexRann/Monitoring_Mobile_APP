package com.example.wewatchyou

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.wewatchyou.databinding.FragmentSecondBinding
import com.example.wewatchyou.global.*
import kotlinx.android.synthetic.main.fragment_second.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs



/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment(), SensorEventListener {

    private var _binding: FragmentSecondBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    // Accéléromètre
    private lateinit var sensorManager : SensorManager

    // Camera
    private var imageCapture : ImageCapture? = null
    private lateinit var cameraExecutor : ExecutorService



    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View? {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)

        setUpSensorStuff ()

        return binding.root
    }



    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // But de ce bouton : quitter l'examen
        binding.buttonSecond.setOnClickListener {
            communication_cs(globalIP, globalPort.toInt(), 4)

            globalConnected = false
            globalIP = ""
            globalPort = ""
            globalCodeEleve = ""
            globalImageBureau = false
            i_accel = 0

            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }

        startCamera()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }





    /* ========================================================================================================================
       ========================================================================================================================
       ==================================================== ACCELEROMETRE ===================================================== */

    // https://www.youtube.com/watch?v=xcsuDDQHrLo
    private fun setUpSensorStuff () {
        sensorManager = layoutInflater.context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener( this, it, 1000000 ) // Approx. every 1 seconds
            //sensorManager.registerListener( this, it, SensorManager.SENSOR_DELAY_NORMAL )
        }
    }



    @RequiresApi(Build.VERSION_CODES.O)
    override fun onSensorChanged(event: SensorEvent?) {
        if ( event?.sensor?.type == Sensor.TYPE_ACCELEROMETER ) {
            //findViewById<TextView>(R.id.id_text_wifi_state).text = accel[0] + ", " + accel[1] + ", " + accel[2]

            accel [0] = event.values[0]
            accel [1] = event.values[1]
            accel [2] = event.values[2]

            // On remet la variable à "Aucun"
            globalMessage_mvt = "Face"

            // https://developer.android.com/guide/topics/sensors/sensors_motion
            var seuil_haut_bas = 1
            // Si on détecte un mouvement (haut, bas, droite ou gauche),
            // on compare la moyenne des valeurs initiales avec celles en cours, si on a une différence (axe X ou axe Z)
            // avec on a un mouvement à droite/gauche ou haut/bas

            if ( (accel[2] != initial_position_mean[2] && abs(accel[2]-initial_position_mean[2]) > seuil_haut_bas) ) {
                globalMessage_mvt = if ( (accel[2] != initial_position_mean[2] && accel[2]-initial_position_mean[2] > seuil_haut_bas) )
                    "Bas" else "Haut"
            }

            var seuil_gauche_droite = 0.5
            if ( (accel[0] != initial_position_mean[0] && abs(accel[0]-initial_position_mean[0]) > seuil_gauche_droite) ) {
                // Si c'est différent de "Face", c'est qu'il y a eu détection de mouvement Haut ou Bas
                if ( globalMessage_mvt != "Face" )
                    globalMessage_mvt += if ( (accel[0] != initial_position_mean[0] && accel[0]-initial_position_mean[0] > seuil_gauche_droite) )
                        "-Droite" else "-Gauche"
                else
                    globalMessage_mvt = if ( (accel[0] != initial_position_mean[0] && accel[0]-initial_position_mean[0] > seuil_gauche_droite) )
                        "Droite" else "Gauche"
            }

            if ( globalPermCam and globalConnected )
            {
                takePhoto()
                communication_cs(globalIP, globalPort.toInt(), 0)
            }
        }
    }



    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    /* ==================================================== ACCELEROMETRE =====================================================
       ========================================================================================================================
       ======================================================================================================================== */





    /* ========================================================================================================================
       ========================================================================================================================
       =================================================== CAMERA =============================================================
       ================================================== FRAGMENT ============================================================ */

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
                image.close()
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
    // https://www.androidbugfix.com/2022/01/android-camerax-image-capture.html
    private fun imageProxyToBitmap (image: ImageProxy): Bitmap {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }



    // Image au format Bitmap à String Base64
    // https://stackoverflow.com/questions/9224056/android-bitmap-to-base64-string
    private fun encodeImage(bm: Bitmap): String? {
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 60, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.NO_WRAP)
    }



    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

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
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

                /*
                imageCapture!!.takePicture(cameraExecutor, object :
                    ImageCapture.OnImageCapturedCallback() {

                    // https://stackoverflow.com/questions/70164601/android-camerax-image-capture-onimagesaved-never-runs
                    override fun onCaptureSuccess(image: ImageProxy) {
                        // From ImageProxy To Bitmap
                        val bitmap = imageProxyToBitmap(image)
                        // From Bitmap to String Base64
                        val imageStr = encodeImage( rotateImage(bitmap, 90f)!! )
                        globalImg = imageStr.toString()
                        image.close()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        cameraExecutor.shutdownNow()
                        super.onError(exception)
                    }
                })
                 */

            } catch (exc: Exception) {
                Log.e(">>> Camera", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor( requireContext()) )
    }

/* ====================================================== FRAGMENT ========================================================
   ======================================================= CAMERA =========================================================
   ========================================================================================================================
   ======================================================================================================================== */



    override fun onDestroyView() {
        super.onDestroyView()

        sensorManager.unregisterListener(this)
        cameraExecutor.shutdown()

        _binding = null
    }



}