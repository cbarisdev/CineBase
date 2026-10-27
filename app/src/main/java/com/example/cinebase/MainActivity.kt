package com.example.cinebase

import com.example.cinebase.model.Movie
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var movieAdapter: MovieAdapter
    private lateinit var db: FirebaseFirestore

    // Arama UI
    private lateinit var searchRow: View
    private lateinit var edtSearch: EditText
    private var btnClearSearch: View? = null

    // Liste cache
    private val allMovies = ArrayList<Movie>()

    // Firestore collection adı
    private val MOVIE_COLLECTION = "Movie"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Views
        recyclerView = findViewById(R.id.recyclerViewMovies)
        progressBar = findViewById(R.id.progressBar)
        val imgProfile = findViewById<ImageView>(R.id.imgProfileIcon)

        // Search views
        searchRow = findViewById(R.id.searchRow)
        edtSearch = findViewById(R.id.edtSearch)

        // btnClearSearch = findViewById<View>(R.id.btnClearSearch)

        // Firestore
        val firebaseApp = FirebaseApp.initializeApp(this)
        if (firebaseApp != null) {
            db = FirebaseFirestore.getInstance(firebaseApp)
        }

        // RecyclerView
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        movieAdapter = MovieAdapter(arrayListOf()) { selectedMovie ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("GonderilenFilm", selectedMovie)
            startActivity(intent)
        }
        recyclerView.adapter = movieAdapter

        // Profil
        imgProfile.setOnClickListener {
            if (::db.isInitialized) {
                startActivity(Intent(this, ProfileActivity::class.java))
            } else {
                Toast.makeText(this, R.string.firebase_not_configured, Toast.LENGTH_LONG).show()
            }
        }

        // Arama satırına dokunulunca klavyeyi aç
        searchRow.setOnClickListener {
            edtSearch.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(edtSearch, InputMethodManager.SHOW_IMPLICIT)
        }

        // Arama - anlık filtre
        edtSearch.addTextChangedListener(SimpleTextWatcher { text ->
            val q = text.trim()
            applyFilter(q)
        })

        btnClearSearch?.setOnClickListener { edtSearch.text?.clear() }

        // Arama satırı scroll ile kaybolsun / geri gelsin
        setupSearchHideOnScroll()

        // Verileri çek
        if (::db.isInitialized) {
            filmleriGetir()
        } else {
            progressBar.visibility = View.GONE
            findViewById<TextView>(R.id.txtSetupMessage).visibility = View.VISIBLE
        }
    }

    private fun filmleriGetir() {
        progressBar.visibility = View.VISIBLE

        db.collection(MOVIE_COLLECTION)
            .get()
            .addOnSuccessListener { documents ->
                val yeniFilmListesi = ArrayList<Movie>()

                for (document in documents) {
                    try {
                        val movie = document.toObject(Movie::class.java)
                        movie.documentId = document.id

                        // Geçerli veri filtreleme
                        if (movie.movieImageUrl.isNotEmpty() &&
                            movie.movieImageUrl.startsWith("http") &&
                            movie.movieName.isNotEmpty()
                        ) {
                            yeniFilmListesi.add(movie)
                        } else {
                            Log.w("Filtre", "${document.id} id'li film bozuk -> elendi")
                        }
                    } catch (e: Exception) {
                        Log.e("Hata", "Çevirme hatası: ${e.message}")
                    }
                }

                // Cache
                allMovies.clear()
                allMovies.addAll(yeniFilmListesi)

                // Arama varsa filtrelenmiş listeyi göster
                val q = edtSearch.text?.toString()?.trim().orEmpty()
                applyFilter(q)

                progressBar.visibility = View.GONE
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Hata: ${exception.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
                progressBar.visibility = View.GONE
                Log.e("FirestoreError", exception.toString())
            }
    }

    private fun applyFilter(query: String) {
        if (query.isBlank()) {
            movieAdapter.updateList(ArrayList(allMovies))
            return
        }

        val qLower = query.lowercase()
        val filtered = allMovies.filter { m ->
            m.movieName.lowercase().contains(qLower) ||
                    m.movieDescription.lowercase().contains(qLower) ||
                    m.releaseYear.toString().contains(qLower)
        }

        movieAdapter.updateList(ArrayList(filtered))
    }

    private var searchHidden = false
    private var scrollAccumulator = 0
    private var isAnimating = false

    private val HIDE_THRESHOLD = 80 // px
    private val SHOW_THRESHOLD = 60 // px

    private fun setupSearchHideOnScroll() {
        searchRow.alpha = 0.75f
        searchRow.translationY = 0f

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (isAnimating) return

                // Kullanıcı yazıyorsa / focus varsa saklama
                if (edtSearch.hasFocus() && edtSearch.text?.isNotEmpty() == true) return

                // Yön değiştiğinde birikimi sıfırla
                if ((dy > 0 && scrollAccumulator < 0) ||
                    (dy < 0 && scrollAccumulator > 0)
                ) {
                    scrollAccumulator = 0
                }

                scrollAccumulator += dy

                if (!searchHidden && scrollAccumulator > HIDE_THRESHOLD) {
                    hideSearchSmooth()
                    scrollAccumulator = 0
                } else if (searchHidden && scrollAccumulator < -SHOW_THRESHOLD) {
                    showSearchSmooth()
                    scrollAccumulator = 0
                }
            }
        })
    }

    private fun hideSearchSmooth() {
        if (searchHidden || isAnimating) return
        isAnimating = true

        searchRow.animate()
            .alpha(0f)
            .translationY(-searchRow.height.toFloat() - 8f)
            .setDuration(200)
            .withEndAction {
                searchHidden = true
                isAnimating = false
            }
            .start()
    }

    private fun showSearchSmooth() {
        if (!searchHidden || isAnimating) return
        isAnimating = true

        searchRow.animate()
            .alpha(0.75f)
            .translationY(0f)
            .setDuration(200)
            .withEndAction {
                searchHidden = false
                isAnimating = false
            }
            .start()
    }

    class SimpleTextWatcher(
        private val onTextChanged: (String) -> Unit
    ) : TextWatcher {
        override fun beforeTextChanged(
            s: CharSequence?, start: Int, count: Int, after: Int
        ) { }

        override fun onTextChanged(
            s: CharSequence?, start: Int, before: Int, count: Int
        ) {
            onTextChanged(s?.toString().orEmpty())
        }

        override fun afterTextChanged(s: Editable?) { }
    }
}
