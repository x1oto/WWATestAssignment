package com.example.wwatestassignment.utils

import kotlin.time.Duration

enum class GameLevel(val obstaclesSpawnDuration: Int) {
    EASY(2000), MEDIUM(1500), HARD(1000)
}