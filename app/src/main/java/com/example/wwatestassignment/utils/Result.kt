package com.example.wwatestassignment.utils

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class Result(val text: String): Parcelable {
    WIN("You win!"), LOSE("You lose!")
}