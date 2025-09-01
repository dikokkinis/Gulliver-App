package com.example.gulliver
import java.util.UUID

/**
 * Represents an item in the packing list.
 *
 * @property id A unique identifier for the item, useful for RecyclerView updates.
 * @property text The description of the item (e.g., "Toothbrush").
 * @property isPacked A boolean indicating whether the user has packed this item.
 */
data class Item(
    val id: String = UUID.randomUUID().toString(), // Unique ID
    var text: String,
    var isPacked: Boolean = false
)