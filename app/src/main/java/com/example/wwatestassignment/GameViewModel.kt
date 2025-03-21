package com.example.wwatestassignment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val _score = MutableStateFlow(0)
    val score get() = _score.asStateFlow()

    private val _lives = MutableStateFlow(3)
    val lives get() = _lives.asStateFlow()

    private var timerJob: Job? = null

    fun increment(startPos: Int, maxHeight: Int): Flow<Float> = flow {
        for (updatedPos in startPos..maxHeight) {
            delay(2)
            emit(updatedPos.toFloat())
        }
    }

    fun decrement(startPos: Int): Flow<Float> = flow {
        for (updatedPos in startPos downTo 0) {
            delay(2)
            emit(updatedPos.toFloat())
        }
    }

    fun decrementLife() {
        val current = _lives.value
        _lives.value = current - 1
    }

    fun incrementLife() {
        if(_lives.value < 3) {
            _lives.value = _lives.value++
        }
    }

    fun startScoreCounter() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val startScore = _score.value
            for (i in startScore until 100) {
                delay(1000)
                _score.value = i + 1
            }
        }
    }

    fun destroyScoreCounter() {
        timerJob?.cancel()
    }
}