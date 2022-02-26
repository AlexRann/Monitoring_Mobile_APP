package com.example.wewatchyou

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.navigation.fragment.findNavController
import com.example.wewatchyou.databinding.FragmentFirstBinding
import com.example.wewatchyou.global.*
import java.util.regex.Pattern



/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    // Message d'erreur lorsque les champs pour la connexion ne sont pas conformes
    private var error_message_connexion : String = ""

    // Réponse du serveur, en fonction du code étudiant fourni par l'élève
    private var check_code = "bad"
    private var check_connexion = false


    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View? {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }



    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val input_ip = view.findViewById<EditText>(R.id.id_input_ip).text
        val input_port = view.findViewById<EditText>(R.id.id_input_port).text
        val input_code_e = view.findViewById<EditText>(R.id.id_input_code_etudiant).text

        // Variable qui va valoir 'true' si il y a possibilité de se connecter selon nos critères, 'false' sinon
        var canConnect = false

        // But de ce bouton : se connecter
        binding.buttonFirst.setOnClickListener {

            globalIP = input_ip.toString()
            globalPort = input_port.toString()
            globalCodeEleve = input_code_e.toString()

            // Si les conditions de connection ne sont pas rencontrées sur :
            // les champs d'entrées (IP, Port, Code étudiant) ou que l'application n'a pas l'autorisation d'utiliser la caméra
            if ( globalIP.isEmpty() || globalPort.isEmpty() || globalCodeEleve.isEmpty() || !Pattern.matches("[0-9]+", globalPort) ||
                !Pattern.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+", globalIP) ||
                !globalPermCam )
            {
                canConnect = false

                if ( globalIP.isEmpty() )
                    error_message_connexion += "\n+ Adresse IP ne doit pas être vide"
                else
                    if ( !Pattern.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+", globalIP) )
                        error_message_connexion += "\n+ Adresse IP doit être au format W.X.Y.Z"

                if ( globalPort.isEmpty() )
                    error_message_connexion += "\n+ Port ne doit pas être vide"
                else
                    if ( !Pattern.matches("[0-9]*", globalPort) )
                        error_message_connexion += "\n+ Port ne doit contenir que des chiffres"

                if ( globalCodeEleve.isEmpty() )
                    error_message_connexion += "\n+ Doit contenir votre code élève, fournit par votre secrétariat ou votre chargé"

                if ( !globalPermCam )
                    error_message_connexion += "\n+ L'application doit avec l'autorisation d'accès à la caméra"
            }
            // Si toutes les conditions précédentes sont rencontrées, alors on peut envoyer des messages au serveur
            // (il nous reste à check si le code étudiant existe)
            else
            {
                var ret = communication_cs(globalIP, globalPort.toInt(), 2)

                check_code = ret.bonCodeEtudiant
                check_connexion = ret.connexionPossible


                // Si le code étudiant n'est pas dans la base de données, alors pas possible de se connecter
                if ( check_code == "bad" ) {
                    canConnect = false
                    error_message_connexion = "\n+ Code étudiant éroné ou non présent !"
                    error_message_connexion += "\n+ (et/ou) Adresse IP du serveur non trouvé !"
                    error_message_connexion += "\n+ (et/ou) Port du serveur non trouvé !"
                }
                else
                    canConnect = true
            }

            if ( !canConnect ) {
                view.findViewById<TextView>(R.id.text_error_connexion).text = error_message_connexion
                Toast.makeText(activity,"Connexion impossible !",Toast.LENGTH_SHORT).show()
            }
            else {
                globalConnected = true
                findNavController().navigate(R.id.action_FirstFragment_to_preExamFragment)
            }
            error_message_connexion = ""
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}