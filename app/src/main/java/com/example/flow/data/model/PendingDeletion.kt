package com.example.flow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A "tombstone" table. When an item is deleted locally, its ID is stored here.
 * This tells the sync logic not to re-add the item from Firebase.
 * It also acts as a queue for items that need to be deleted from Firebase.
 */
@Entity(tableName = "pending_deletions")
data class PendingDeletion(
    @PrimaryKey
    val id: String, // The ID of the item (Task, Note, or Transaction)

    // We store the collection name to know which Firebase collection to delete from
    val collectionName: String
)