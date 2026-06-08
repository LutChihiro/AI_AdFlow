package com.example.aiadflow.data.model

enum class Channel(
    val id: String,
    val title: String
) {
    Featured("featured", "Featured"),
    Ecommerce("ecommerce", "Ecommerce"),
    Local("local", "Local"),
    NewArrival("new_arrival", "New Arrival"),
    Finance("finance", "Finance"),
    Health("health", "Health"),
    Travel("travel", "Travel"),
    Education("education", "Education")
}
