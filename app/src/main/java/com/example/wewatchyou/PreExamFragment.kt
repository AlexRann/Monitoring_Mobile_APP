package com.example.wewatchyou

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageCapture
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.wewatchyou.databinding.PreExamFragmentBinding
import com.example.wewatchyou.global.*
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class PreExamFragment : Fragment(), SensorEventListener {

    private var _binding : PreExamFragmentBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    // Message d'instructions à suivre pour passer à la phase d'examen
    private var instructions : String = ""

    // Accéléromètre
    private lateinit var sensorManager : SensorManager

    // Camera
    private var imageCapture : ImageCapture? = null
    private lateinit var cameraExecutor : ExecutorService
    private val REQUEST_CODE = 42



    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View? {
        _binding = PreExamFragmentBinding.inflate(inflater, container, false)

        setUpSensorStuff()

        return binding.root
    }



    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        instructions += "A faire obligatoirement, sinon il vous sera impossible de passer l'examen\n\n\n"
        instructions += "Instructions pour une bonne surveillance pendant l'examen :\n\n"
        instructions += "1) Prenez une photo de votre environnement de travail.\n"
        instructions += "Pour cela, il faudra au préalable ne laisser sur votre bureau que le matériel nécessaire et de retirer tout objet de vos murs.\n\n"
        instructions += "2) Placez votre smartphone sur le front, puis appuyez sur le bouton <Fixer PC>. " +
                "Ensuite, fixez l'écran de votre PC pendant 3 secondes.\n\n"
        instructions += "3) Vous pouvez passer à l'examen surveillé, mais avant, n'oubliez de placer votre smartphone sur votre front, avec l'écran face à l'ordinateur.\n"

        binding.idTextProtocoleHeader.text = "ATTENTION\nA LIRE ATTENTIVEMENT"
        binding.idTextProtocoleBody.text = instructions

        // Passer à l'étape finale, mode examen
        binding.buttonToExam.setOnClickListener {
            if ( i_accel == 5 && globalImageBureau && globalConnected && globalPermCam ) {
                communication_cs(globalIP, globalPort.toInt(), 5)
                findNavController().navigate( R.id.action_preExamFragment_to_SecondFragment )
            }
            else
                Snackbar.make(view, "Veuillez effectuer les instructions citées", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        // Se déconnecter (pour éventuellement changer de compte si l'élève s'est trompé-e)
        binding.buttonDeco.setOnClickListener {
            communication_cs(globalIP, globalPort.toInt(), 3)

            globalConnected = false
            globalIP = ""
            globalPort = ""
            globalCodeEleve = ""
            globalImageBureau = false
            i_accel = 0

            findNavController().navigate(R.id.action_preExamFragment_to_FirstFragment)
        }

        // Activer le test position initiale (fixer PC)
        binding.buttonFixeCamera.setOnClickListener {
            Snackbar.make(view, "Fixez l'écran de votre PC", Snackbar.LENGTH_LONG).setAction("Action", null).show()

            i_accel = 0
        }

        // Activer le test photo bureau
        binding.buttonPhotoBureau.setOnClickListener {
            val takePictureIntent = Intent( MediaStore.ACTION_IMAGE_CAPTURE )
            val packageManager = requireActivity().packageManager

            if ( takePictureIntent.resolveActivity(packageManager) != null ) {
                startActivityForResult(takePictureIntent, REQUEST_CODE)
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }



    /* ========================================================================================================================
   ========================================================================================================================
   ==================================================== ACCELEROMETRE ===================================================== */

    // https://www.youtube.com/watch?v=xcsuDDQHrLo
    private fun setUpSensorStuff () {
        sensorManager = layoutInflater.context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener( this, it, SensorManager.SENSOR_DELAY_NORMAL ) // Approx. every 1 seconds
        }
    }



    override fun onSensorChanged(event: SensorEvent?) {
        if ( event?.sensor?.type == Sensor.TYPE_ACCELEROMETER ) {
            //findViewById<TextView>(R.id.id_text_wifi_state).text = accel[0] + ", " + accel[1] + ", " + accel[2]

            if ( i_accel < 5 ) {
                // On demande à l'élève de fixer son écran pendant 5 secondes
                // ça va nous permettre de connaître sa position initiale
                // On demandera à l'élève de ne pas bouger la position du PC pendant toute l'épreuve
                while ( i_accel < 5 ) {
                    initial_position.set( i_accel, floatArrayOf(event.values[0], event.values[1], event.values[2]) )
                    i_accel++
                }

                // On additionne toutes chaque valeurs à leur coordonnées respectifs
                initial_position.forEach { e ->
                    initial_position_mean[0] += e!![0]
                    initial_position_mean[1] += e!![1]
                    initial_position_mean[2] += e!![2]
                }

                // On divise le tout pour obtenir la moyenne
                initial_position_mean[0] /= initial_position.size.toFloat()
                initial_position_mean[1] /= initial_position.size.toFloat()
                initial_position_mean[2] /= initial_position.size.toFloat()
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if ( requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK ) {
            val takenImage = data?.extras?.get("data") as Bitmap

            globalImageBureau = true

            globalImg = encodeImage(takenImage).toString()
            communication_cs(globalIP, globalPort.toInt(), 1)
        }
        else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    // Image au format Bitmap à String Base64
    private fun encodeImage(bm: Bitmap): String? {
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.NO_WRAP)
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