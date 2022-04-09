package com.example.statereducerflow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.statereducerflow.StateReducerFlow
import com.example.statereducerflow.data.copySelectionFrom
import com.example.statereducerflow.data.toggleSelection
import com.example.statereducerflow.logic.FetchMovies
import com.example.statereducerflow.ui.MoviesListEvent.*
import kotlinx.coroutines.launch

class MoviesListViewModel(
    val fetchMovies: FetchMovies = FetchMovies()
) : ViewModel() {

    val state = StateReducerFlow(
        initialState = MoviesListState.initial,
        reduceState = ::reduceState,
    )

    private fun reduceState(
        currentState: MoviesListState,
        event: MoviesListEvent
    ): MoviesListState = when (event) {
        is ScreenStarted,
        is RefreshClicked -> {
            refreshMovies()
            currentState.copy(isLoading = true)
        }

        is MovieClicked -> {
            val clickedId = event.movie.id
            val updatedMovies = currentState.movies.toggleSelection(clickedId)
            currentState.copy(movies = updatedMovies)
        }

        is ShuffleClicked -> {
            val shuffledMovies = currentState.movies.shuffled()
            currentState.copy(movies = shuffledMovies)
        }

        is MoviesLoaded -> {
            val newMovies = event.movies
            val currentMovies = currentState.movies
            val updatedMovies = newMovies.copySelectionFrom(currentMovies)
            currentState.copy(isLoading = false, movies = updatedMovies)
        }
    }

    private fun refreshMovies() = viewModelScope.launch {
        val movies = fetchMovies()
        state.handleEvent(MoviesLoaded(movies))
    }
}
