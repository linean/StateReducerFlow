package com.example.statereducerflow

import com.example.statereducerflow.data.Movie
import com.example.statereducerflow.logic.FetchMovies
import com.example.statereducerflow.ui.MoviesListEvent
import com.example.statereducerflow.ui.MoviesListEvent.*
import com.example.statereducerflow.ui.MoviesListState
import com.example.statereducerflow.ui.MoviesListViewModel
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private fun Movie(id: Int = 1) = Movie(id, "name", "image")

class MoviesListViewModelTest {

    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    private val fetchMovies = mockk<FetchMovies>()
    private lateinit var state: StateReducerFlow<MoviesListState, MoviesListEvent>

    @Before
    fun setUp() {
        val viewModel = MoviesListViewModel(fetchMovies)
        state = viewModel.state
    }

    @Test
    fun `fetches movies after screen is started`() {
        state.handleEvent(ScreenStarted)

        coVerify { fetchMovies() }
        assertEquals(true, state.value.isLoading)
    }

    @Test
    fun `fetches movies after refresh clicked`() {
        state.handleEvent(RefreshClicked)

        coVerify { fetchMovies() }
        assertEquals(true, state.value.isLoading)
    }

    @Test
    fun `displays movies after loaded`() {
        val testMovies = (0..2).map(::Movie)
        state.handleEvent(MoviesLoaded(testMovies))

        assertEquals(false, state.value.isLoading)
        assertEquals(testMovies, state.value.movies)
    }

    @Test
    fun `toggles selection after movie clicked`() {
        fun isSelected() = state.value.movies.first().isSelected
        val testMovie = Movie()

        state.handleEvent(MoviesLoaded(listOf(testMovie)))
        assertEquals(false, isSelected())

        state.handleEvent(MovieClicked(testMovie))
        assertEquals(true, isSelected())

        state.handleEvent(MovieClicked(testMovie))
        assertEquals(false, isSelected())
    }

    @Test
    fun `persist selection across updates`() {
        fun isSelected() = state.value.movies.first().isSelected
        val testMovie = Movie()

        state.handleEvent(MoviesLoaded(listOf(testMovie)))
        assertEquals(false, isSelected())

        state.handleEvent(MovieClicked(testMovie))
        assertEquals(true, isSelected())

        state.handleEvent(MoviesLoaded(listOf(testMovie)))
        assertEquals(true, isSelected())
    }

    @Test
    fun `shuffles movies after shuffle clicked`() {
        val movies = (0..50).map(::Movie)
        state.handleEvent(MoviesLoaded(movies))
        assertEquals(movies, state.value.movies)

        state.handleEvent(ShuffleClicked)

        assertEquals(movies.size, state.value.movies.size)
        assertNotSame(movies, state.value.movies)
    }
}
