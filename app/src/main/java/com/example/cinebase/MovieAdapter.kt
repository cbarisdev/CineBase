
package com.example.cinebase

import com.example.cinebase.model.Movie
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MovieAdapter(
    private val movieList: ArrayList<Movie>,
    private val onMovieClick: (Movie) -> Unit
) : RecyclerView.Adapter<MovieAdapter.MovieHolder>() {

    class MovieHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imagePoster: ImageView =
            itemView.findViewById(R.id.ivMoviePoster)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MovieHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movie, parent, false)

        return MovieHolder(view)
    }

    override fun onBindViewHolder(holder: MovieHolder, position: Int) {
        val movie = movieList[position]

        // Film posterini yükle
        Glide.with(holder.itemView.context)
            .load(movie.movieImageUrl)
            .error(android.R.drawable.stat_notify_error)
            .centerCrop()
            .into(holder.imagePoster)

        // Film detayına geçiş
        holder.itemView.setOnClickListener {
            onMovieClick(movie)
        }
    }

    override fun getItemCount(): Int {
        return movieList.size
    }

    fun updateList(newList: List<Movie>) {
        movieList.clear()
        movieList.addAll(newList)
        notifyDataSetChanged()
    }
}
