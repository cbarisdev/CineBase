
package com.example.cinebase

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val edtNickname = findViewById<TextInputEditText>(R.id.edtNickname)
        val edtEmail = findViewById<TextInputEditText>(R.id.edtEmail)
        val edtPassword = findViewById<TextInputEditText>(R.id.edtPassword)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegisterComplete)
        val txtBackToLogin = findViewById<TextView>(R.id.txtBackToLogin)

        // Giriş sayfasına dönmek isterse
        txtBackToLogin.setOnClickListener {
            finish()
        }

        // Kayıt ol butonu
        btnRegister.setOnClickListener {

            val nickname = edtNickname.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (nickname.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    this,
                    "Lütfen tüm alanları doldurun",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(
                    this,
                    "Şifre en az 6 karakter olmalı",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // 1. Firebase'de kullanıcı oluştur
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->

                    // 2. Kullanıcı adını (nickname) profile kaydet
                    val user: FirebaseUser? = authResult.user

                    val profileUpdates: UserProfileChangeRequest =
                        UserProfileChangeRequest.Builder()
                            .setDisplayName(nickname)
                            .build()

                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { task ->

                            if (task.isSuccessful) {
                                // Her şey başarılı! Ana sayfaya git
                                Toast.makeText(
                                    this,
                                    "Hoşgeldin $nickname!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                val intent = Intent(
                                    this,
                                    MainActivity::class.java
                                )

                                // Geri tuşuna basınca kayıt ekranına dönmesin
                                intent.flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK

                                startActivity(intent)
                            }
                        }
                }
                .addOnFailureListener { exception ->

                    Toast.makeText(
                        this,
                        "Hata: ${exception.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }
}
