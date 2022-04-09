package com.example.statereducerflow.data

import kotlinx.serialization.Serializable

@Serializable
data class Movie(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val isSelected: Boolean = false
)

fun List<Movie>.copySelectionFrom(movies: List<Movie>): List<Movie> {
    val selectedIds = movies.filter { it.isSelected }.map { it.id }.toSet()
    return map { movie -> movie.copy(isSelected = selectedIds.contains(movie.id)) }
}

fun List<Movie>.toggleSelection(movieId: Int) = map { movie ->
    if (movie.id == movieId) movie.copy(isSelected = !movie.isSelected)
    else movie
}
