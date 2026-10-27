
package com.example.cinebase

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale
import com.example.cinebase.model.Comment
class CommentsAdapter(
    private val commentList: ArrayList<Comment>,
    private val onLikeClick: (Comment) -> Unit,
    private val onLongClick: (Comment) -> Unit
) : RecyclerView.Adapter<CommentsAdapter.CommentHolder>() {

    class CommentHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val txtUser: TextView =
            itemView.findViewById(R.id.txtCommentUser)

        val txtText: TextView =
            itemView.findViewById(R.id.txtCommentText)

        val txtLikeCount: TextView =
            itemView.findViewById(R.id.txtLikeCount)

        val imgLike: ImageView =
            itemView.findViewById(R.id.imgLike)

        val layoutLike: LinearLayout =
            itemView.findViewById(R.id.layoutLike)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CommentHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)

        return CommentHolder(view)
    }

    override fun onBindViewHolder(
        holder: CommentHolder,
        position: Int
    ) {
        val comment = commentList[position]
        val currentUserId: String? =
            FirebaseAuth.getInstance().currentUser?.uid

        holder.txtUser.text =
            comment.userName.uppercase(Locale.getDefault())

        holder.txtText.text = comment.text
        holder.txtLikeCount.text = comment.likeCount.toString()

        // Beğeni rengini ayarla
        if (currentUserId != null &&
            comment.likedBy.contains(currentUserId)
        ) {
            holder.imgLike.setImageResource(
                R.drawable.ic_favorite_filled
            )
            holder.imgLike.setColorFilter(
                Color.parseColor("#E50914")
            )
            holder.txtLikeCount.setTextColor(
                Color.parseColor("#E50914")
            )
        } else {
            holder.imgLike.setImageResource(
                R.drawable.ic_favorite_border
            )
            holder.imgLike.setColorFilter(
                Color.parseColor("#80FFFFFF")
            )
            holder.txtLikeCount.setTextColor(
                Color.parseColor("#80FFFFFF")
            )
        }

        // Tıklama: beğenme
        holder.layoutLike.setOnClickListener {
            onLikeClick(comment)
        }

        // Uzun basma: silme
        holder.itemView.setOnLongClickListener {
            onLongClick(comment)
            true
        }
    }

    override fun getItemCount(): Int {
        return commentList.size
    }
}
