package com.example.statereducerflow.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.statereducerflow.R
import com.example.statereducerflow.data.Movie
import com.example.statereducerflow.ui.MoviesListEvent.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

class MoviesListActivity : ComponentActivity() {

    private val viewModel: MoviesListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.state.collectAsState()
            MoviesListScreen(state, viewModel.state::handleEvent)
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.state.handleEvent(ScreenStarted)
    }
}

@Composable
private fun MoviesListScreen(
    state: MoviesListState,
    handleEvent: (MoviesListEvent) -> Unit
) = MaterialTheme {
    Box(modifier = Modifier.fillMaxSize()) {
        MoviesList(
            title = state.title,
            movies = state.movies,
            isLoading = state.isLoading,
            handleEvent = handleEvent
        )

        ShuffleButton(
            handleEvent = handleEvent,
            modifier = Modifier.align(BottomEnd)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MoviesList(
    title: String,
    movies: List<Movie>,
    isLoading: Boolean,
    handleEvent: (MoviesListEvent) -> Unit
) {
    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = isLoading),
        onRefresh = { handleEvent(RefreshClicked) }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 78.dp)
        ) {
            item {
                MoviesHeader(title)
            }

            items(
                items = movies,
                key = Movie::id
            ) { movie ->
                Movie(
                    movie = movie,
                    handleEvent = handleEvent,
                    modifier = Modifier.animateItemPlacement()
                )
            }
        }
    }
}

@Composable
private fun MoviesHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.h3,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
private fun Movie(
    movie: Movie,
    handleEvent: (MoviesListEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { handleEvent(MovieClicked(movie)) }
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        AsyncImage(
            model = movie.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp, 64.dp)
                .background(MaterialTheme.colors.onSurface.copy(alpha = 0.2F))
        )

        Text(
            text = movie.name,
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )

        val imageResource = when (movie.isSelected) {
            true -> R.drawable.ic_star
            false -> R.drawable.ic_star_outline
        }

        Icon(
            painter = painterResource(imageResource),
            contentDescription = null,
            tint = Color(0xFFFFC107),
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun ShuffleButton(
    handleEvent: (MoviesListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_shuffle),
        contentDescription = null,
        tint = Color.White,
        modifier = modifier
            .padding(16.dp)
            .size(56.dp)
            .shadow(4.dp, CircleShape)
            .background(MaterialTheme.colors.secondary)
            .clip(CircleShape)
            .clickable { handleEvent(ShuffleClicked) }
            .padding(12.dp)
    )
}
