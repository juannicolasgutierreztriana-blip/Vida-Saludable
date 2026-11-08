package com.example.vidasaludable.model

data class Routine(
    var id: Long,
    var name: String,
    var time: String,
    var emoji: String,
    var done: Boolean
)
