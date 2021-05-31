package com.example.picturecomposer

data class Photo(
    val url: String = "",
    val subject: String = "",
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
    val thirds: Int = 0,
    val framing: Int = 0,
    val exposure: Int = 0,
    val level: Int = 0,
    val blur: Int = 0,
    val grade: Int = 0,
    val key: String = ""
)