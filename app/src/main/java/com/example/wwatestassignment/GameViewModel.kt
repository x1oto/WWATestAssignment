package com.example.wwatestassignment

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GameViewModel : ViewModel() {
    fun increment(startPos: Int, maxHeight: Int): Flow<Float> = flow {
        for(updatedPos in startPos..maxHeight) {
            delay(2)
            emit(updatedPos.toFloat())
        }
    }

    fun decrement(startPos: Int): Flow<Float> = flow {
        for(updatedPos in startPos downTo 0) {
            delay(2)
            emit(updatedPos.toFloat())
        }
    }
}