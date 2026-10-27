
package com.example.cinebase.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class Movie(
    var documentId: String = "",
    val movieName: String = "",
    val movieImageUrl: String = "",
    val movieDescription: String = "",
    val movieRating: Double = 0.0,
    val releaseYear: Int = 0
) : Parcelable
