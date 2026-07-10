package com.example.hycoms

data class HistoryModel(
    val timestamp: String,
    val airTemp: String,
    val humidity: String,
    val ph: String,
    val ppm: String,
    val waterLevel: String,
    val waterTemp: String
)