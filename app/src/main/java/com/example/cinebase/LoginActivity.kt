
package com.example.cinebase

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Kullanıcı zaten giriş yapmışsa ana sayfaya geç
        if (auth.currentUser != null) {
            anaSayfayaGit()
            return
        }

        // Arayüz elemanları
        val edtEmail = findViewById<TextInputEditText>(R.id.edtEmail)
        val edtPassword = findViewById<TextInputEditText>(R.id.edtPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)
        val txtForgotPassword = findViewById<TextView>(R.id.txtForgotPassword)

        // Giriş yap
        btnLogin.setOnClickListener {

            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {

                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Tekrar Hoşgeldin!",
                            Toast.LENGTH_SHORT
                        ).show()

                        anaSayfayaGit()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(
                            this,
                            "Giriş Başarısız: ${exception.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

            } else {
                Toast.makeText(
                    this,
                    "Lütfen e-posta ve şifrenizi girin.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Kayıt ol ekranı
        btnRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Şifremi unuttum
        txtForgotPassword.setOnClickListener {
            Toast.makeText(
                this,
                "Bu özellik yakında eklenecek!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Ana sayfaya geçiş
    private fun anaSayfayaGit() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
