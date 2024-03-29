package com.example.wewatchyou.global

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.WeWatchYou.CommunicationGrpc
import com.example.WeWatchYou.ReplyComm
import com.example.WeWatchYou.RequestComm
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit


var globalIP : String = ""
var globalPort : String = ""
var globalCodeEleve : String = ""
var globalConnected : Boolean = false
var globalImageBureau : Boolean = false

// gRPC
lateinit var channel : ManagedChannel

// Type d'envoie
// Data : envoies de données précises au serveur (accéléromètre, image, etc)
// Connexion : envoie un test de connexion au serveur pour vérifier si les paramètres sont bons
// Déconnexion : se déconnecte, n'a pas passé l'examen
// Fin : se déconnecte, a passé l'examen
// Début : commence l'examen
val type_envoie = arrayOf ( "Data", "PhotoBureau", "Connexion", "Déconnexion", "Fin", "Début" )

// Accelerometer
var i_accel = 0 // Permet de prendre uniquement les 5 premières itérations de l'accéléromètre
val initial_position = arrayOfNulls<FloatArray>(5) // Coordonnées de la position de l'élève pendant 3 secondes
val initial_position_mean : Array<Float> = Array(3) { 0f } // La moyenne des coordonnées de l'élève pendant les 3 secondes
val accel : Array<Float> = Array(3) { 0f } // Coordonnées en continue
var globalMessage_mvt : String = "Face" // Si on détecte un mouvement, "Droite/Gauche/Haut/Bas", sinon "Face"

// Camera
var globalPermCam : Boolean = false
var globalImg : String = ""



/**
 * Valeurs possibles des variables :
 *  - connexionPossible : true, false
 *  - bonCodeEtudiant : good, bad
 */
data class retConnexionCS ( var connexionPossible : Boolean, var bonCodeEtudiant : String )

/**
 * Paramètres
 *  nb_command (Int) : le type d'envoie au serveur que l'on souhaite faire
 *
 * Return
 *  ret0 (Boolean) : renvoi une valeur qui dépend si l'envoie d'un signal au serveur a été possible ou pas
 *              true : envoie possible, false, envoie pas possible
 *  ret1 (String) : envoi possible au serveur mais si le code étudiant n'existe pas, alors "bad", sinon "good"
 * */
// Reprise de la structure sur le site : https://medium.com/swlh/a-beginners-guide-to-grpc-in-android-61cc56a423f7
@RequiresApi(Build.VERSION_CODES.O)
fun communication_cs (IP : String, port : Int, nb_command : Int ) : retConnexionCS {
    var ret0 = false
    var ret1 = "bad"

    if ( nb_command < type_envoie.size ) {
        // Si c'est un test de connexion ou une déconnexion, alors on n'envoie rien d'autre que le message de notification
        if ( nb_command > 1 ) {
            globalMessage_mvt = ""
            globalImg = ""
        }

        try  {
            // ======================================= DEBUT, REPRISE DU CODE =======================================
            channel = ManagedChannelBuilder.forAddress(IP, port)
                .usePlaintext()
                .build()

            val type_action = type_envoie [ nb_command ]

            val stub : CommunicationGrpc.CommunicationBlockingStub = CommunicationGrpc
                .newBlockingStub(channel)
            // ============================== DEBUT, MODIFICATION APPORTEE ==============================
                .withDeadlineAfter(2, TimeUnit.SECONDS)

            val request : RequestComm = RequestComm.newBuilder()
                .setTypeMessage( type_action )
                .setCodeEtudiant( globalCodeEleve )
                .setMsgAccel( globalMessage_mvt )
                .setImg( globalImg )
                .setDateTime( LocalDateTime.now().format( DateTimeFormatter.ofPattern("dd-MM-YYYY_HH-mm-ss")).toString() )
                .build()
            // ============================== DEBUT, MODIFICATION APPORTEE ==============================

            val reply : ReplyComm = stub.sendComm(request)
            // ======================================= FIN, REPRISE DU CODE =======================================
            ret0 = true
            ret1 = reply.checkConnexion

            //channel.shutdownNow()
        } catch ( e : Exception ) {
            //
            ret0 = false
        }
    }

    return retConnexionCS( ret0, ret1 )
}



class globalVariables {
}

