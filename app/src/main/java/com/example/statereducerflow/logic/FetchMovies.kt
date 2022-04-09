package com.example.statereducerflow.logic

import com.example.statereducerflow.data.Movie
import com.example.statereducerflow.data.moviesJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.random.Random

class FetchMovies {

    suspend operator fun invoke(): List<Movie> = withContext(Dispatchers.IO) {
        delay(Random.nextLong(300, 2000))
        Json.decodeFromString(moviesJson)
    }
}
