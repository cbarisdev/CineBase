
package com.example.cinebase

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale
import com.example.cinebase.model.Comment
class MyCommentsAdapter(
    private val comments: ArrayList<Comment>,
    private val onCommentClick: (String) -> Unit,
    private val onCommentLongClick: (Comment) -> Unit
) : RecyclerView.Adapter<MyCommentsAdapter.Holder>() {

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val txtMovieName: TextView =
            itemView.findViewById(R.id.txtCommentUser)

        val txtContent: TextView =
            itemView.findViewById(R.id.txtCommentText)

        val txtLikeCount: TextView =
            itemView.findViewById(R.id.txtLikeCount)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)

        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {

        val item = comments[position]

        // Profilde kullanıcı adı yerine film adını göster
        holder.txtMovieName.text =
            item.movieName.uppercase(Locale.getDefault())

        holder.txtContent.text = item.text
        holder.txtLikeCount.text = item.likeCount.toString()

        // Kısa tıklama: Film detayına git
        holder.itemView.setOnClickListener {
            onCommentClick(item.movieId)
        }

        // Uzun tıklama: Yorum silme işlemi
        holder.itemView.setOnLongClickListener {
            onCommentLongClick(item)
            true
        }
    }

    override fun getItemCount() = comments.size
}
