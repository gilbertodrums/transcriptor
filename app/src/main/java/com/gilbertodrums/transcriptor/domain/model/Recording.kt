package com.gilbertodrums.transcriptor.domain.model

import java.time.LocalDateTime

data class Recording(
    val id: String,
    val title: String,
    val createdAt: LocalDateTime,
    val filePath: String,
    val transcript: String? = null
)
