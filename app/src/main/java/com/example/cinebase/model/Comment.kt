
package com.example.cinebase.model

data class Comment(
    var id: String = "",
    val movieId: String = "",
    val movieName: String = "",
    val userName: String = "",
    val userId: String = "",
    val text: String = "",
    val timestamp: Long = 0,
    val likeCount: Int = 0,
    val likedBy: ArrayList<String> = ArrayList()
)
