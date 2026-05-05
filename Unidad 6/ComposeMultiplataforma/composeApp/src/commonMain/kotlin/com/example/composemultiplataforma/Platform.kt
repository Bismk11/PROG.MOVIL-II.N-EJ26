package com.example.composemultiplataforma

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform