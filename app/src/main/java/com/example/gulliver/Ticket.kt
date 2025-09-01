package com.example.gulliver

data class Ticket(
    val from: String,
    val to: String,
    val date: String,
    val departureTime: String,
    val arrivalTime: String,
    val pdfPath: String
)
