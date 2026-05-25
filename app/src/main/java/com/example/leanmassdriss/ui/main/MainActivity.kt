package com.example.leanmassdriss.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.leanmassdriss.ui.calculator.CalculatorActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {

            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (user == null) {
                val intent = Intent(this, com.example.leanmassdriss.ui.auth.LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
                return
            }

            // Redirection vers le calculateur
            val intent = Intent(this, CalculatorActivity::class.java)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            // Self-healing : On déconnecte et on retourne au login en cas de corruption de session
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            } catch (inner: Exception) {}
            
            val intent = Intent(this, com.example.leanmassdriss.ui.auth.LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
