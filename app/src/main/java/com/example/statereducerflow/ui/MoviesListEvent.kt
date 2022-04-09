package com.example.statereducerflow.ui

import com.example.statereducerflow.data.Movie

sealed class MoviesListEvent {
    object ScreenStarted : MoviesListEvent()
    object RefreshClicked : MoviesListEvent()
    object ShuffleClicked : MoviesListEvent()
    data class MoviesLoaded(val movies: List<Movie>) : MoviesListEvent()
    data class MovieClicked(val movie: Movie) : MoviesListEvent()
}
