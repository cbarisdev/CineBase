package com.example.cinebase

import com.example.cinebase.model.Movie
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Locale
import com.example.cinebase.model.Comment
class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Görünümler
    private lateinit var recyclerFavorites: RecyclerView
    private lateinit var lblFavorites: TextView
    private lateinit var btnComments: MaterialButton
    private lateinit var btnBack: MaterialButton

    // Durum kontrolü (true = favoriler, false = yorumlar)
    private var isShowingFavorites = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        val userId: String? = auth.currentUser?.uid

        // Tanımlamalar
        recyclerFavorites = findViewById(R.id.recyclerFavorites)
        lblFavorites = findViewById(R.id.lblFavorites)
        btnComments = findViewById(R.id.btnComments)
        btnBack = findViewById(R.id.btnBack)

        val txtUsername: TextView = findViewById(R.id.txtUsername)
        val btnLogout: MaterialButton = findViewById(R.id.btnLogout)

        // Kullanıcı adını göster
        auth.currentUser?.let { user ->
            val name: String = user.displayName
                ?: user.email?.substringBefore('@')
                ?: "Kullanıcı"
            txtUsername.text = name.uppercase(Locale.getDefault())
        }

        // Başlangıç: favorileri yükle
        loadFavorites(userId)

        // Yorumlar butonu
        btnComments.setOnClickListener {
            if (isShowingFavorites) {
                loadMyComments(userId)
            } else {
                Toast.makeText(
                    this,
                    "Zaten yorumlarınızdasınız.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Geri butonu
        btnBack.setOnClickListener {
            if (!isShowingFavorites) {
                // Yorumlardaysak favorilere dön
                loadFavorites(userId)
            } else {
                // Favorilerdeysek sayfayı kapat
                finish()
            }
        }

        // Çıkış butonu
        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    // Favorileri yükle
    private fun loadFavorites(userId: String?) {
        if (userId == null) return

        isShowingFavorites = true
        lblFavorites.text = "Favori Filmlerim"
        recyclerFavorites.adapter = null

        recyclerFavorites.layoutManager = GridLayoutManager(this, 3)

        db.collection("Users").document(userId)
            .collection("Favorites")
            .get()
            .addOnSuccessListener { documents ->
                val favList = ArrayList<Movie>()
                for (doc in documents) {
                    try {
                        val movie: Movie = doc.toObject(Movie::class.java)
                        movie.documentId = doc.id
                        favList.add(movie)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Favori adapter'ı
                recyclerFavorites.adapter = MovieAdapter(favList) { movie ->
                    val intent = Intent(this, DetailActivity::class.java)
                    intent.putExtra("GonderilenFilm", movie)
                    startActivity(intent)
                }
            }
    }

    // Yorumları yükle
    private fun loadMyComments(userId: String?) {
        if (userId == null) return

        isShowingFavorites = false
        lblFavorites.text = "Yaptığım Yorumlar"
        recyclerFavorites.adapter = null

        recyclerFavorites.layoutManager = LinearLayoutManager(this)

        db.collection("Users").document(userId)
            .collection("MyComments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val myComments = ArrayList<Comment>()
                for (doc in documents) {
                    try {
                        val c: Comment = doc.toObject(Comment::class.java)
                        // Garanti olsun diye ID'yi doküman ID'sinden ata
                        c.id = doc.id
                        myComments.add(c)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (myComments.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Henüz hiç yorum yapmamışsın.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                recyclerFavorites.adapter = MyCommentsAdapter(
                    myComments,
                    onCommentClick = { movieId ->
                        fetchAndOpenMovie(movieId)
                    },
                    onCommentLongClick = { commentToDelete ->
                        showDeleteDialog(userId, commentToDelete)
                    }
                )
            }
    }

    // Silme onay kutusu
    private fun showDeleteDialog(userId: String, comment: Comment) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Yorumu Sil")
        builder.setMessage("Bu yorumu kalıcı olarak silmek istiyor musun?")

        builder.setPositiveButton("EVET, SİL") { _, _ ->
            deleteComment(userId, comment)
        }

        builder.setNegativeButton("VAZGEÇ") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    // Firebase silme işlemi
    private fun deleteComment(userId: String, comment: Comment) {
        // 1. Kullanıcının profilinden sil
        db.collection("Users").document(userId)
            .collection("MyComments").document(comment.id)
            .delete()
            .addOnSuccessListener {
                // 2. Filmin altından da sil (film ID'si varsa)
                if (comment.movieId.isNotEmpty()) {
                    db.collection("Movie").document(comment.movieId)
                        .collection("Comments").document(comment.id)
                        .delete()
                }

                Toast.makeText(
                    this,
                    "Yorum başarıyla silindi.",
                    Toast.LENGTH_SHORT
                ).show()

                // Listeyi yenile
                loadMyComments(userId)
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Silinirken hata oluştu!",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // Yoruma tıklayınca film verisini çekip aç
    private fun fetchAndOpenMovie(movieId: String) {
        if (movieId.isEmpty()) return

        db.collection("Movie").document(movieId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val movie: Movie? = doc.toObject(Movie::class.java)
                    if (movie != null) {
                        movie.documentId = doc.id
                        val intent = Intent(this, DetailActivity::class.java)
                        intent.putExtra("GonderilenFilm", movie)
                        startActivity(intent)
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Bu film artık mevcut değil.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}
