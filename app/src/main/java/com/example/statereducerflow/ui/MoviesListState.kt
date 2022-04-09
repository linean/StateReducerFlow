package com.example.statereducerflow.ui

import com.example.statereducerflow.data.Movie

data class MoviesListState(
    val isLoading: Boolean,
    val title: String,
    val movies: List<Movie>
) {

    companion object {
        val initial = MoviesListState(
            isLoading = false,
            title = "IMDb\nTop 10 Movies",
            movies = emptyList()
        )
    }
}
