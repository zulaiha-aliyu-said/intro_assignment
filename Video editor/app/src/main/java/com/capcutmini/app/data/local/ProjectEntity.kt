package com.capcutmini.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sourceUri: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val durationMs: Long = 0L,
    /** Serialized [com.capcutmini.app.media.model.EditDocument] including fades/transitions/subtitles. */
    val editJson: String = "{}"
)
