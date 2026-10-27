package com.example.cinebase

import com.example.cinebase.model.Comment
import com.example.cinebase.model.Movie
import android.app.AlertDialog
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class DetailActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var isFavorite = false

    private lateinit var commentsAdapter: CommentsAdapter
    private val commentList = ArrayList<Comment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // 1. Firebase başlat
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // 2. Görünümler
        val imgPoster: ImageView = findViewById(R.id.imgDetailPoster)
        val btnBack: MaterialButton = findViewById(R.id.btnBack)
        val btnFavorite: MaterialButton = findViewById(R.id.btnFavorite)

        val txtTitle: TextView = findViewById(R.id.txtDetailTitle)
        val txtRating: TextView = findViewById(R.id.txtDetailRating)
        val txtYear: TextView = findViewById(R.id.txtDetailYear)
        val txtDesc: TextView = findViewById(R.id.txtDetailDescription)

        // Yorum alanları
        val edtComment: TextInputEditText = findViewById(R.id.edtComment)
        val btnSendComment: MaterialButton = findViewById(R.id.btnSendComment)
        val recyclerComments: RecyclerView = findViewById(R.id.recyclerComments)

        // 3. Veriyi al
        val movie: Movie? = intent.getParcelableExtra("GonderilenFilm")

        movie?.let { currentMovie ->

            // UI doldur
            txtTitle.text = currentMovie.movieName
            txtDesc.text = currentMovie.movieDescription
            txtRating.text = "⭐ ${currentMovie.movieRating}"
            txtYear.text = currentMovie.releaseYear.toString()

            Glide.with(this)
                .load(currentMovie.movieImageUrl)
                .centerCrop()
                .into(imgPoster)

            // Geri dön
            btnBack.setOnClickListener { finish() }

            // Favori kontrolü
            checkFavoriteStatus(currentMovie.documentId, btnFavorite)
            btnFavorite.setOnClickListener { toggleFavorite(currentMovie, btnFavorite) }

            // Yorum sistemi kurulumu
            commentsAdapter = CommentsAdapter(
                commentList,
                onLikeClick = { clickedComment ->
                    toggleCommentLike(currentMovie.documentId, clickedComment)
                },
                onLongClick = { clickedComment ->
                    val currentUserId: String? = auth.currentUser?.uid
                    // Sadece kendi yazdığımız yorumu silebiliriz
                    if (currentUserId != null && clickedComment.userId == currentUserId) {
                        showDeleteDialog(clickedComment, currentMovie.documentId)
                    } else {
                        Toast.makeText(this, "Sadece kendi yorumunu silebilirsin!", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            recyclerComments.layoutManager = LinearLayoutManager(this)
            recyclerComments.adapter = commentsAdapter

            // Yorumları getir
            loadComments(currentMovie.documentId)

            // Yorum gönder butonu
            btnSendComment.setOnClickListener {
                val commentText: String = edtComment.text.toString().trim()

                if (commentText.isNotEmpty()) {
                    val user = auth.currentUser
                    val userName: String = user?.displayName
                        ?: user?.email?.substringBefore("@")
                        ?: "Misafir"
                    val uid: String = user?.uid ?: ""

                    // ID oluştur
                    val newCommentRef = db.collection("Movie")
                        .document(currentMovie.documentId)
                        .collection("Comments")
                        .document()
                    val commentId: String = newCommentRef.id

                    val newComment = Comment(
                        id = commentId,
                        movieId = currentMovie.documentId,
                        movieName = currentMovie.movieName,
                        userName = userName,
                        userId = uid,
                        text = commentText,
                        timestamp = System.currentTimeMillis()
                    )

                    // 1. Filmin altına kaydet
                    newCommentRef.set(newComment).addOnSuccessListener {

                        // 2. Kullanıcının profiline kaydet
                        if (uid.isNotEmpty()) {
                            db.collection("Users").document(uid)
                                .collection("MyComments").document(commentId).set(newComment)
                        }

                        Toast.makeText(this, "Yorum paylaşıldı!", Toast.LENGTH_SHORT).show()
                        edtComment.text?.clear()

                        // Listeyi güncelle
                        loadComments(currentMovie.documentId)

                        // Klavyeyi kapat
                        try {
                            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.hideSoftInputFromWindow(edtComment.windowToken, 0)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    Toast.makeText(this, "Yorum boş olamaz!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Silme onay kutusu
    private fun showDeleteDialog(comment: Comment, movieId: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Yorumu Sil")
        builder.setMessage("Bu yorumu silmek istediğine emin misin?")
        builder.setPositiveButton("SİL") { _, _ ->
            deleteComment(comment, movieId)
        }
        builder.setNegativeButton("VAZGEÇ", null)
        builder.show()
    }

    // Silme işlemi (hem filmden hem profilden)
    private fun deleteComment(comment: Comment, movieId: String) {
        // 1. Film koleksiyonundan sil
        db.collection("Movie").document(movieId)
            .collection("Comments").document(comment.id)
            .delete()
            .addOnSuccessListener {

                // 2. Kullanıcı profilinden sil
                val uid: String? = auth.currentUser?.uid
                if (uid != null) {
                    db.collection("Users").document(uid)
                        .collection("MyComments").document(comment.id)
                        .delete()
                }

                Toast.makeText(this, "Yorum silindi.", Toast.LENGTH_SHORT).show()

                // Listeyi anında güncelle
                loadComments(movieId)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Hata oluştu.", Toast.LENGTH_SHORT).show()
            }
    }

    // Yorumları getir
    private fun loadComments(movieId: String) {
        db.collection("Movie").document(movieId).collection("Comments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                commentList.clear()
                for (doc in documents) {
                    val comment: Comment = doc.toObject(Comment::class.java)
                    // ID'yi garantiye al
                    comment.id = doc.id
                    commentList.add(comment)
                }
                commentsAdapter.notifyDataSetChanged()
            }
    }

    // Favori kontrolü
    private fun checkFavoriteStatus(movieId: String, btn: MaterialButton) {
        val userId: String = auth.currentUser?.uid ?: return
        if (movieId.isEmpty()) return

        db.collection("Users").document(userId)
            .collection("Favorites").document(movieId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    isFavorite = true
                    btn.setIconResource(R.drawable.ic_favorite_filled)
                } else {
                    isFavorite = false
                    btn.setIconResource(R.drawable.ic_favorite_border)
                }
            }
    }

    // Favori ekle/çıkar
    private fun toggleFavorite(movie: Movie, btn: MaterialButton) {
        val userId: String = auth.currentUser?.uid ?: return
        val docRef = db.collection("Users").document(userId)
            .collection("Favorites").document(movie.documentId)

        if (isFavorite) {
            docRef.delete().addOnSuccessListener {
                isFavorite = false
                btn.setIconResource(R.drawable.ic_favorite_border)
                Toast.makeText(this, "Favorilerden çıkarıldı", Toast.LENGTH_SHORT).show()
            }
        } else {
            docRef.set(movie).addOnSuccessListener {
                isFavorite = true
                btn.setIconResource(R.drawable.ic_favorite_filled)
                Toast.makeText(this, "Favorilere eklendi ❤️", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Beğeni (like) işlemi
    private fun toggleCommentLike(movieId: String, comment: Comment) {
        val userId: String = auth.currentUser?.uid ?: return
        if (comment.id.isEmpty()) return

        val commentRef = db.collection("Movie").document(movieId)
            .collection("Comments").document(comment.id)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(commentRef)
            val currentLikeCount: Long = snapshot.getLong("likeCount") ?: 0
            @Suppress("UNCHECKED_CAST")
            val currentLikedBy: ArrayList<String> =
                snapshot.get("likedBy") as? ArrayList<String> ?: ArrayList()

            if (currentLikedBy.contains(userId)) {
                currentLikedBy.remove(userId)
                transaction.update(commentRef, "likedBy", currentLikedBy)
                transaction.update(commentRef, "likeCount", currentLikeCount - 1)
            } else {
                currentLikedBy.add(userId)
                transaction.update(commentRef, "likedBy", currentLikedBy)
                transaction.update(commentRef, "likeCount", currentLikeCount + 1)
            }
        }.addOnSuccessListener {
            loadComments(movieId)
        }
    }
}
