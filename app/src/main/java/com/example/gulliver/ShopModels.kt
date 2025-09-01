package com.example.gulliver



// Represents a single shop with its details.
data class Shop(
    val name: String?,
    val location: String?,
    val rating: String?,
    val imageUri: String?
)

// Represents a single category, containing a title and a list of shops.
data class ShopCategory(
    var title: String,
    val shops: MutableList<Shop> = mutableListOf()
)