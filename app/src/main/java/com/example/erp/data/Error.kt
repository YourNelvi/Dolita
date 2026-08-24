package com.example.erp.data

sealed interface Error {
    data class NetworkError(val message: String) : Error
    data class ApiError(val source: String, val code: Int, val message: String) : Error
    data class ParseError(val field: String, val message: String) : Error
}