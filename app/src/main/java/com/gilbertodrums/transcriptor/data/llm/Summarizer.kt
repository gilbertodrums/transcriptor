package com.gilbertodrums.transcriptor.data.llm

data class Summary(
    val points: List<String>,
    val isAi: Boolean   // false = resumen básico extractivo; true = generado por IA local
)

interface Summarizer {
    suspend fun summarize(text: String): Summary
}
