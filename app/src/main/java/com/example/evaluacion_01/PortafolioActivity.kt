package com.example.evaluacion_01

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.URI

class PortafolioActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_portafolio)

        val tvGithub= findViewById<TextView>(R.id.tvGithub)
        val tvLinkedin= findViewById<TextView>(R.id.tvLinkedin)

        // Github
        tvGithub.setOnClickListener {
            val url = "https://github.com/Player-spect/"
            val Intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(Intent)
        }

        // Linkedin
         tvLinkedin.setOnClickListener {
            val url = "https://www.linkedin.com/in/freddy-nuñez-856181388/"
            val Intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
             startActivity(Intent)
        }
    }
}